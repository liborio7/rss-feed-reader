(ns rss-feed-reader.domain.feed
  (:require [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as spec]
            [rss-feed-reader.utils.time :as time]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

(def table :feed)

;; model

(spec/def :feed.domain/id uuid?)
(spec/def :feed.domain/version nat-int?)
(spec/def :feed.domain/order-id nat-int?)
(spec/def :feed.domain/insert-time ::time/time)
(spec/def :feed.domain/update-time (spec/nilable ::time/time))
(spec/def :feed.domain/link uri?)

(spec/def ::model (spec/keys :req [:feed.domain/id
                                   :feed.domain/version
                                   :feed.domain/order-id
                                   :feed.domain/insert-time
                                   :feed.domain/update-time
                                   :feed.domain/link]))

(spec/def ::create-model (spec/keys :req [:feed.domain/link]
                                    :opt [:feed.domain/id]))

;; conversion

(defn model->db [model]
  (let [now (time/instant-now)
        {:feed.domain/keys [id link]
         :or               {id (UUID/randomUUID)}} model]
    {:feed/id          id
     :feed/version     0
     :feed/order-id    (time/instant->long now)
     :feed/insert-time now
     :feed/update-time nil
     :feed/link        (str link)}))

(defn db->model [model]
  (let [{:feed/keys [id version order-id insert-time update-time link]} model]
    {:feed.domain/id          id
     :feed.domain/version     version
     :feed.domain/order-id    order-id
     :feed.domain/insert-time insert-time
     :feed.domain/update-time update-time
     :feed.domain/link        (uris/from-string link)}))

;; component

(defprotocol Feeds
  (get-all [this] [this opts])
  (get-by-id [this model])
  (get-by-ids [this models])
  (get-by-link [this model])
  (create! [this model])
  (delete! [this model]))

(defrecord DbFeeds [datasource]
  Feeds

  ;; get

  (get-all [this]
    (get-all this {:starting-after 0}))

  (get-all [_ {:keys [starting-after]}]
    (log/debug "get all starting after" starting-after)
    (let [select #(db/with-transaction [conn datasource]
                    (db/select-values conn table {:where    [:> :feed/order-id %]
                                                  :order-by [[:feed/order-id :asc]]
                                                  :limit    20}))
          db-models (db/paginate-select select :feed/order-id starting-after)]
      (map db->model db-models)))

  (get-by-id [_ model]
    (log/debug "get by id" model)
    (db/with-transaction [conn datasource]
      (let [id (:feed.domain/id model)
            db-model (db/select conn table {:where [:= :feed/id id]})]
        (when db-model
          (db->model db-model)))))

  (get-by-ids [_ models]
    (log/debug "get by" (count models) "ids")
    (db/with-transaction [conn datasource]
      (let [ids (map :feed.domain/id models)
            db-models (when (not-empty ids)
                        (db/select-values conn table {:where [:in :feed/id ids]}))]
        (mapv db->model db-models))))

  (get-by-link [_ model]
    (log/debug "get by link" model)
    (db/with-transaction [conn datasource]
      (let [link (:feed.domain/link model)
            db-model (db/select conn table {:where [:= :feed/link (str link)]})]
        (when db-model
          (db->model db-model)))))

  ;; create

  (create! [this model]
    (log/info "create" model)
    (let [errors (specs/errors ::create-model model)]
      (if (not-empty errors)
        (do
          (log/info "invalid request" errors)
          (throw (ex-info "invalid request"
                          {:cause   :feed-create
                           :reason  :invalid-spec
                           :details errors})))
        (if-let [feed (get-by-link this model)]
          feed
          (db/with-transaction [tx datasource]
            (->> model
                 (model->db)
                 (db/insert! tx table)
                 (db->model)))))))

  ;; delete

  (delete! [_ model]
    (log/info "delete" model)
    (db/with-transaction [tx datasource]
      (let [id (:feed.domain/id model)]
        (db/delete! tx table {:where [:= :feed/id id]})))))