(ns rss-feed-reader.domain.feed-item
  (:require [rss-feed-reader.db.postgres :refer [ds]]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.sql :as sql]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as t]
            [rss-feed-reader.utils.uri :as uris])
  (:import (java.util UUID)))

(def table :feed_item)

;; model

(s/def :feed.item.domain/id uuid?)
(s/def :feed.item.domain/version nat-int?)
(s/def :feed.item.domain/order-id nat-int?)
(s/def :feed.item.domain/insert-time ::t/time)
(s/def :feed.item.domain/update-time (s/nilable ::t/time))
(s/def :feed.item.domain/feed :rss-feed-reader.domain.feed/model)
(s/def :feed.item.domain/title string?)
(s/def :feed.item.domain/link uri?)
(s/def :feed.item.domain/pub-time ::t/time)
(s/def :feed.item.domain/description (s/nilable string?))

(s/def ::model (s/keys :req [:feed.item.domain/id
                             :feed.item.domain/version
                             :feed.item.domain/order-id
                             :feed.item.domain/insert-time
                             :feed.item.domain/update-time
                             :feed.item.domain/feed
                             :feed.item.domain/title
                             :feed.item.domain/link
                             :feed.item.domain/pub-time
                             :feed.item.domain/description]))

(s/def ::create-model (s/keys :req [:feed.item.domain/feed
                                    :feed.item.domain/title
                                    :feed.item.domain/link
                                    :feed.item.domain/pub-time]
                              :opt [:feed.item.domain/id
                                    :feed.item.domain/description]))
;; conversion

(defn model->db [model]
  (let [now (t/instant-now)
        {:feed.item.domain/keys [id feed title link pub-time description]
         :or                    {id       (UUID/randomUUID)
                                 pub-time now}} model]
    {:feed.item/id          id
     :feed.item/version     0
     :feed.item/order_id    (t/instant->long pub-time)
     :feed.item/insert_time now
     :feed.item/update_time nil
     :feed.item/feed_id     (:feed.domain/id feed)
     :feed.item/title       title
     :feed.item/link        (str link)
     :feed.item/pub_time    pub-time
     :feed.item/description description}))

(defn db->model
  ([model]
   (let [{:feed.item/keys [feed_id]} model
         feed (feeds/get-by-id {:feed.domain/id feed_id})]
     (db->model model feed)))
  ([model feed]
   (let [{:feed.item/keys [id version order_id insert_time update_time title link pub_time description]} model]
     {:feed.item.domain/id          id
      :feed.item.domain/version     version
      :feed.item.domain/order-id    order_id
      :feed.item.domain/insert-time insert_time
      :feed.item.domain/update-time update_time
      :feed.item.domain/feed        feed
      :feed.item.domain/title       title
      :feed.item.domain/link        (uris/from-string link)
      :feed.item.domain/pub-time    pub_time
      :feed.item.domain/description description})))

;; get

(defn get-all [& {:keys [starting-after limit]
                  :or   {starting-after 0 limit 20}}]
  (log/debug "get all starting-after" starting-after "limit" limit)
  (let [db-models (sql/paginate
                    #(sql/select-values ds table {:where    [:> :feed.item/order_id %]
                                                  :order-by [[:feed.item/order_id :asc]]
                                                  :limit    20})
                    :feed.item/order_id
                    starting-after)]
    (map db->model db-models)))

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:feed.item.domain/id model)
        db-model (sql/select ds table {:where [:= :feed.item/id id]})]
    (when db-model
      (db->model db-model))))

(defn get-by-feed [model & {:keys [starting-after limit]
                            :or   {starting-after 0 limit 20}}]
  (log/debug "get by feed" model "starting-after" starting-after "limit" limit)
  (let [feed (:feed.item.domain/feed model)
        feed-id (:feed.domain/id feed)
        db-models (sql/paginate
                    #(sql/select-values ds table {:where    [:and
                                                             [:= :feed.item/feed_id feed-id]
                                                             [:> :feed.item/order_id %]]
                                                  :order-by [[:feed.item/order_id :asc]]
                                                  :limit    20})
                    :feed.item/order_id
                    starting-after)
        db->model* (fn [db-model]
                     (db->model db-model feed))]
    (map db->model* db-models)))

(defn get-by-link [model]
  (log/debug "get by link" model)
  (let [link (str (:feed.item.domain/link model))
        db-model (sql/select ds table {:where [:= :feed.item/link link]})]
    (when db-model
      (db->model db-model))))

(defn get-by-links [models]
  (log/debug "get by" (count models) "links")
  (let [links (->> models
                   (map :feed.item.domain/link)
                   (map str))
        feed-by-feed-id (->> models
                             (map :feed.item.domain/feed)
                             (mapcat (juxt :feed.domain/id identity))
                             (apply hash-map))
        db-models (when (not-empty links)
                    (sql/select-values ds table {:where [:in :feed.item/link links]}))
        db->model* (fn [db-model]
                     (let [feed (get feed-by-feed-id (:feed.item/feed_id db-model))]
                       (db->model db-model feed)))]
    (map db->model* db-models)))

;; create

(defn create! [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed-item (get-by-link model)]
        feed-item
        (->> model
             (model->db)
             (sql/insert! ds table)
             (db->model))))))

(defn create-multi! [models]
  (log/info "create" (count models) "model(s)")
  (let [errors (specs/errors (s/coll-of ::create-model) models)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [feed-by-feed-id (->> models
                                 (map :feed.item.domain/feed)
                                 (mapcat (juxt :feed.domain/id identity))
                                 (apply hash-map))
            existing-models (get-by-links models)
            existing-links (map :feed.item.domain/link existing-models)
            missing-models (remove
                             (fn [model]
                               (let [link (:feed.item.domain/link model)]
                                 (some (partial = link) existing-links)))
                             models)]
        (concat
          existing-models
          (let [db->model* (fn [db-model]
                             (let [feed (get feed-by-feed-id (:feed.item/feed_id db-model))]
                               (db->model db-model feed)))]
            (when (not-empty missing-models)
              (->> missing-models
                   (map model->db)
                   (sql/insert-values! ds table)
                   (map db->model*)))))))))

;; delete

(defn delete! [model]
  (log/info "delete" model)
  (let [id (:feed.item.domain/id model)]
    (sql/delete! ds table {:where [:= :feed.item/id id]})))