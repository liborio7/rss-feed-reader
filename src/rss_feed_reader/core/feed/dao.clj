(ns rss-feed-reader.core.feed.dao
  (:require [rss-feed-reader.db.postgres :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def ds @db/datasource)
(def table :feed)

;; model

(s/def :feed/id uuid?)
(s/def :feed/version nat-int?)
(s/def :feed/order_id nat-int?)
(s/def :feed/insert_time inst?)
(s/def :feed/update_time (s/nilable inst?))
(s/def :feed/link string?)

(s/def ::model (s/keys :req [:feed/id
                             :feed/version
                             :feed/order_id
                             :feed/insert_time
                             :feed/update_time
                             :feed/link]))

;; get

(defn get-by-id [model]
  (sql/get-by-id ds table :feed/id model))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-ids [models]
  (if (empty? models)
    []
    (sql/get-multi-by-id ds table :feed/id models)))

(s/fdef get-by-ids
        :args (s/cat :models (s/coll-of (s/keys :req [:feed/id])))
        :ret (s/coll-of ::model))

(defn get-by-link [{:feed/keys [link]}]
  (sql/get-by-query ds table {:where [:= :feed/link link]}))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed/link]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-all [& {:keys [starting-after limit]
                  :or   {starting-after 0 limit 20}}]
  (sql/get-multi-by-query ds table {:where    [:> :feed/order_id starting-after]
                                    :order-by [[:feed/order_id :asc]]
                                    :limit    limit}))

(s/fdef get-all
        :args (s/* (s/cat :opt keyword? :val nat-int?))
        :ret (s/coll-of ::model))

;; insert

(defn insert [model]
  (sql/insert ds table :feed/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [model]
  (sql/delete ds table :feed/id model))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed/id]))
        :ret int?)