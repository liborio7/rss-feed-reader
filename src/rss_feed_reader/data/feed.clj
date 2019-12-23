(ns rss-feed-reader.data.feed
  (:require [rss-feed-reader.data.postgres.db :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def db db/connection)
(def table :feed)

;; spec

(s/def :feed/id uuid?)
(s/def :feed/version pos-int?)
(s/def :feed/order_id pos-int?)
(s/def :feed/insert_time inst?)
(s/def :feed/update_time inst?)
(s/def :feed/link string?)

(s/def ::model (s/keys :req [:feed/id
                             :feed/version
                             :feed/order_id
                             :feed/insert_time
                             :feed/update_time
                             :feed/link]))

;; get

(defn get-by-id [model]
  (sql/get-by-id db table :feed/id model))

(s/fdef get-by-id
        :args (s/cat :id :feed/id)
        :ret ::model)

(defn get-by-link [{:feed/keys [link]}]
  (sql/get-by-query db table {:where [:= :feed/link link]}))

(s/fdef get-by-link
        :args (s/cat :link :feed/link)
        :ret ::model)

;; insert

(defn insert [model]
  (sql/insert db table :feed/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [model]
  (sql/delete db table :feed/id model))

(s/fdef delete
        :args (s/cat :id :feed/id)
        :ret (s/and int?))