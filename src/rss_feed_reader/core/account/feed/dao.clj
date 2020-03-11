(ns rss-feed-reader.core.account.feed.dao
  (:require [rss-feed-reader.db.postgres :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def db db/connection)
(def table :account_feed)

;; model

(s/def :account.feed/id uuid?)
(s/def :account.feed/version nat-int?)
(s/def :account.feed/order_id nat-int?)
(s/def :account.feed/insert_time inst?)
(s/def :account.feed/update_time (s/nilable inst?))
(s/def :account.feed/account_id uuid?)
(s/def :account.feed/feed_id uuid?)

(s/def ::model (s/keys :req [:account.feed/id
                             :account.feed/version
                             :account.feed/order_id
                             :account.feed/insert_time
                             :account.feed/update_time
                             :account.feed/account_id
                             :account.feed/feed_id]))

;; get

(defn get-by-id [model]
  (sql/get-by-id db table :account.feed/id model))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account.feed/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-account-id [{:account.feed/keys [account_id]}
                         & {:keys [starting-after limit]
                            :or   {starting-after 0 limit 20}}]
  (sql/get-multi-by-query db table {:where    [:and
                                               [:= :account.feed/account_id account_id]
                                               [:> :account.feed/order_id starting-after]]
                                    :order-by [[:account.feed/order_id :asc]]
                                    :limit    limit}))

(s/fdef get-by-account-id
        :args (s/cat :model (s/keys :req [:account.feed/account_id])
                     :opts (s/* (s/cat :opt keyword? :val nat-int?)))
        :ret (s/coll-of ::model))

(defn get-by-feed-id [{:account.feed/keys [feed_id]}
                      & {:keys [starting-after limit]
                         :or   {starting-after 0 limit 20}}]
  (sql/get-multi-by-query db table {:where    [:and
                                               [:= :account.feed/feed_id feed_id]
                                               [:> :account.feed/order_id starting-after]]
                                    :order-by [[:account.feed/order_id :asc]]
                                    :limit    limit}))

(s/fdef get-by-feed-id
        :args (s/cat :model (s/keys :req [:account.feed/feed_id])
                     :opts (s/* (s/cat :opt keyword? :val nat-int?)))
        :ret (s/coll-of ::model))

(defn get-by-account-id-and-feed-id [{:account.feed/keys [account_id feed_id]}]
  (sql/get-by-query db table {:where [:and [:= :account.feed/account_id account_id] [:= :account.feed/feed_id feed_id]]}))

(s/fdef get-by-account-id-and-feed-id
        :args (s/cat :model (s/keys :req [:account.feed/account_id
                                          :account.feed/feed_id]))
        :ret (s/or :ok ::model :not-found nil?))

;; insert

(defn insert [model]
  (sql/insert db table :account.feed/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [model]
  (sql/delete db table :account.feed/id model))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account.feed/id]))
        :ret int?)
