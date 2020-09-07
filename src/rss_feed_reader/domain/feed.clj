(ns rss-feed-reader.domain.feed
  (:require [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.utils.time :as t]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

(def table :feed)

;; model

(s/def :feed.domain/id uuid?)
(s/def :feed.domain/version nat-int?)
(s/def :feed.domain/order-id nat-int?)
(s/def :feed.domain/insert-time ::t/time)
(s/def :feed.domain/update-time (s/nilable ::t/time))
(s/def :feed.domain/link uri?)

(s/def ::model (s/keys :req [:feed.domain/id
                             :feed.domain/version
                             :feed.domain/order-id
                             :feed.domain/insert-time
                             :feed.domain/update-time
                             :feed.domain/link]))

(s/def ::create-model (s/keys :req [:feed.domain/link]
                              :opt [:feed.domain/id]))

;; conversion

(defn model->db [model]
  (let [now (t/instant-now)
        {:feed.domain/keys [id link]
         :or               {id (UUID/randomUUID)}} model]
    {:feed/id          id
     :feed/version     0
     :feed/order_id    (t/instant->long now)
     :feed/insert_time now
     :feed/update_time nil
     :feed/link        (str link)}))

(defn db->model [model]
  (let [{:feed/keys [id version order_id insert_time update_time link]} model]
    {:feed.domain/id          id
     :feed.domain/version     version
     :feed.domain/order-id    order_id
     :feed.domain/insert-time insert_time
     :feed.domain/update-time update_time
     :feed.domain/link        (uris/from-string link)}))

;; get

(defn get-all [& {:keys [starting-after]
                  :or   {starting-after 0}}]
  (log/debug "get all starting after" starting-after)
  (let [db-models (db/paginate-select
                    #(db/select-values table {:where     [:> :feed/order_id %]
                                                  :order-by [[:feed/order_id :asc]]
                                                  :limit    20})
                    :feed/order_id
                    starting-after)]
    (map db->model db-models)))

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:feed.domain/id model)
        db-model (db/select table {:where [:= :feed/id id]})]
    (when db-model
      (db->model db-model))))

(defn get-by-ids [models]
  (log/debug "get by" (count models) "ids")
  (let [ids (map :feed.domain/id models)
        db-models (when (not-empty ids)
                    (db/select-values table {:where [:in :feed/id ids]}))]
    (map db->model db-models)))

(defn get-by-link [model]
  (log/debug "get by link" model)
  (let [link (:feed.domain/link model)
        db-model (db/select table {:where [:= :feed/link (str link)]})]
    (when db-model
      (db->model db-model))))

;; create

(defn create! [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed (get-by-link model)]
        feed
        (->> model
             (model->db)
             (db/insert! table)
             (db->model))))))

;; delete

(defn delete! [model]
  (log/info "delete" model)
  (let [id (:feed.domain/id model)]
    (db/delete! table {:where [:= :feed/id id]})))