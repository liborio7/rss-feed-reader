(ns rss-feed-reader.domain.account.dao
  (:require [rss-feed-reader.db.postgres :as db]
            [rss-feed-reader.db.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def ds @db/datasource)
(def table :account)

;; model

(s/def :account/id uuid?)
(s/def :account/version nat-int?)
(s/def :account/order_id nat-int?)
(s/def :account/insert_time inst?)
(s/def :account/update_time (s/nilable inst?))
(s/def :account/username (s/nilable string?))
(s/def :account/chat_id int?)

(s/def ::model (s/keys :req [:account/id
                             :account/version
                             :account/order_id
                             :account/insert_time
                             :account/update_time
                             :account/username
                             :account/chat_id]))

;; get

(defn get-by-id [model]
  (sql/get-by-id ds table :account/id model))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-ids [models]
  (if (empty? models)
    []
    (sql/get-multi-by-id ds table :account/id models)))

(s/fdef get-by-ids
        :args (s/cat :models (s/coll-of (s/keys :req [:account/id])))
        :ret (s/coll-of ::model))

(defn get-by-username [{:account/keys [username]}]
  (sql/get-by-query ds table {:where [:= :account/username username]}))

(s/fdef get-by-username
        :args (s/cat :model (s/keys :req [:account/username]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-chat-id [{:account/keys [chat_id]}]
  (sql/get-by-query ds table {:where [:= :account/chat_id chat_id]}))

(s/fdef get-by-chat-id
        :args (s/cat :model (s/keys :req [:account/chat_id]))
        :ret (s/or :ok ::model :not-found nil?))

;; insert

(defn insert [model]
  (sql/insert ds table :account/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [model]
  (sql/delete ds table :account/id model))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account/id]))
        :ret int?)