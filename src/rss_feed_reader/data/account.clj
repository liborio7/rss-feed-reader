(ns rss-feed-reader.data.account
  (:require [rss-feed-reader.data.postgres.db :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def db db/connection)
(def table :account)

;; spec

(s/def :account/id uuid?)
(s/def :account/version pos-int?)
(s/def :account/order_id pos-int?)
(s/def :account/insert_time inst?)
(s/def :account/update_time inst?)
(s/def :account/username string?)

(s/def ::model (s/keys :req [:account/id
                             :account/version
                             :account/order_id
                             :account/insert_time
                             :account/update_time
                             :account/username]))

;; get

(defn get-by-id [model]
  (sql/get-by-id db table :account/id model))

(s/fdef get-by-id
        :args (s/cat :id :account/id)
        :ret ::model)

(defn get-by-username [{:account/keys [username]}]
  (sql/get-multi-by-query db table {:where [:= :account/username username]}))

(s/fdef get-by-username
        :args (s/cat :username :account/username)
        :ret ::model)

;; insert

(defn insert [model]
  (sql/insert db table :account/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [model]
  (sql/delete db table :account/id model))

(s/fdef delete
        :args (s/cat :id :account/id)
        :ret int?)