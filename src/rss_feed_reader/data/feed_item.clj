(ns rss-feed-reader.data.feed_item
  (:require [rss-feed-reader.data.postgres.db :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))
;; utils

(def db db/connection)
(def table :feed_item)

;; spec

(s/def :feed.item/id uuid?)
(s/def :feed.item/version pos-int?)
(s/def :feed.item/order_id pos-int?)
(s/def :feed.item/insert_time inst?)
(s/def :feed.item/update_time inst?)
(s/def :feed.item/feed_id uuid?)
(s/def :feed.item/title string?)
(s/def :feed.item/link uri?)
(s/def :feed.item/pub_time inst?)
(s/def :feed.item/description string?)

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
        :args (s/cat :id :feed.item/id)
        :ret ::model)

(defn get-by-link [{:feed.item/keys [link]}]
  (sql/get-by-query db table {:where [:= :feed.item/link link]}))

(s/fdef get-by-link
        :args (s/cat :link :feed.item/link)
        :ret ::model)

(defn get-by-feed-id [{:feed.item/keys [feed_id]}
                      & {:keys [starting-after limit]
                         :or   {starting-after 0 limit 50}}]
  (sql/get-by-query-multi db table {:where    [:and
                                               [:= :feed.item/feed_id feed_id]
                                               [:> :feed.item/order_id starting-after]]
                                    :order-by [[:feed.item/order_id :desc]]
                                    :limit    limit}))

(s/fdef get-by-feed-id
        :args (s/cat :feed_id :feed.item/feed_id
                     :starting-after :feed.item/starting-after
                     :limit :feed.item/limit)
        :ret (s/coll-of ::model))

;; insert

(defn insert [model]
  (sql/insert db table :feed.item/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [model]
  (sql/delete db table :feed.item/id model))

(s/fdef delete
        :args (s/cat :id :feed.item/id)
        :ret (s/and int?))