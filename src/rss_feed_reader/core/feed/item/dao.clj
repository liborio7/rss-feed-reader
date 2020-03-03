(ns rss-feed-reader.core.feed.item.dao
  (:require [rss-feed-reader.db.postgres :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))
;; utils

(def db db/connection)
(def table :feed_item)

;; model

(s/def :feed.item/id uuid?)
(s/def :feed.item/version nat-int?)
(s/def :feed.item/order_id nat-int?)
(s/def :feed.item/insert_time inst?)
(s/def :feed.item/update_time (s/nilable inst?))
(s/def :feed.item/feed_id uuid?)
(s/def :feed.item/title string?)
(s/def :feed.item/link string?)
(s/def :feed.item/pub_time inst?)
(s/def :feed.item/description (s/nilable string?))

(s/def ::model (s/keys :req [:feed.item/id
                             :feed.item/version
                             :feed.item/order_id
                             :feed.item/insert_time
                             :feed.item/update_time
                             :feed.item/feed_id
                             :feed.item/title
                             :feed.item/link
                             :feed.item/pub_time
                             :feed.item/description]))

;; get

(defn get-by-id [model]
  (sql/get-by-id db table :feed.item/id model))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed.item/id]))
        :ret ::model)

(defn get-by-id-multi [models]
  (if (empty? models)
    []
    (sql/get-multi-by-id db table :feed.item/id models)))

(s/fdef get-by-id-multi
        :args (s/cat :models (s/coll-of (s/keys :req [:feed.item/id])))
        :ret (s/coll-of ::model))

(defn get-by-link [{:feed.item/keys [link]}]
  (sql/get-by-query db table {:where [:= :feed.item/link link]}))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed.item/link]))
        :ret ::model)

(defn get-by-links [models]
  (let [links (->> models
                   (map :feed.item/link)
                   (reduce conj []))]
    (sql/get-multi-by-query db table {:where [:in :feed.item/link links]})))

(s/fdef get-by-links
        :args (s/cat :models (s/coll-of (s/keys :req [:feed.item/link])))
        :ret (s/coll-of ::model))

(defn get-by-feed-id [{:feed.item/keys [feed_id]}
                      & {:keys [starting-after limit]
                         :or   {starting-after 0 limit 20}}]
  (sql/get-multi-by-query db table {:where    [:and
                                               [:= :feed.item/feed_id feed_id]
                                               [:> :feed.item/order_id starting-after]]
                                    :order-by [[:feed.item/order_id :asc]]
                                    :limit    limit}))

(s/fdef get-by-feed-id
        :args (s/cat :model (s/keys :req [:feed.item/feed_id])
                     :starting-after :feed.item/order_id
                     :limit (s/int-in 0 100))
        :ret (s/coll-of ::model))

;; insert

(defn insert [model]
  (sql/insert db table :feed.item/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

(defn insert-multi [models]
  (if (empty? models)
    []
    (sql/insert-multi db table :feed.item/id models)))

(s/fdef insert-multi
        :args (s/cat :model (s/coll-of ::model))
        :ret (s/coll-of ::model))

;; delete

(defn delete [model]
  (sql/delete db table :feed.item/id model))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed.item/id]))
        :ret (s/and int?))