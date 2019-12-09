(ns rss-feed-reader.data.account_feed
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]))

;; utils

(def db db/connection)
(def table "account_feed")
(def opts {:qualifier (clojure.string/replace table "_" ".")})

;; spec

(s/def :account.feed/id uuid?)
(s/def :account.feed/version int?)
(s/def :account.feed/order_id int?)
(s/def :account.feed/insert_time inst?)
(s/def :account.feed/update_time inst?)
(s/def :account.feed/account_id uuid?)
(s/def :account.feed/feed_id uuid?)

(s/def ::model (s/keys :req [:account.feed/id
                             :account.feed/version
                             :account.feed/order_id
                             :account.feed/insert_time
                             :account.feed/update_time
                             :account.feed/account_id
                             :account.feed/feed_id]))

;; get by id

(defn get-by-id [{:account.feed/keys [id]}]
  (log/info "get by id" id)
  (let [result (jdbc/get-by-id db table id :account.feed/id opts)]
    (log/info "result" result)
    result))

(s/fdef get-by-id
        :args (s/cat :id :account.feed/id)
        :ret ::model)

(defn get-by-account-id [{:account.feed/keys [account_id]}]
  (log/info "get by account id" account_id)
  (let [result (-> (jdbc/find-by-keys db table {:account.feed/account_id account_id} opts))]
    (log/info "result count" (count result))
    result))

(s/fdef get-by-account-id
        :args (s/cat :account_id :account.feed/account_id)
        :ret (s/coll-of ::model))

(defn get-by-feed-id [{:account.feed/keys [feed_id]}]
  (log/info "get by feed id" feed_id)
  (let [result (-> (jdbc/find-by-keys db table {:account.feed/feed_id feed_id} opts))]
    (log/info "result count" (count result))
    result))

(s/fdef get-by-feed-id
        :args (s/cat :feed_id :account.feed/feed_id)
        :ret (s/coll-of ::model))

(defn get-by-account-id-and-feed-id [{:account.feed/keys [account_id feed_id]}]
  (log/info "get by account id and feed id" account_id feed_id)
  (let [result (-> (jdbc/find-by-keys db table {:account.feed/account_id account_id :account.feed.domain/feed_id feed_id} opts)
                   (first))]
    (log/info "result" result)
    result))

(s/fdef get-by-account-id-and-feed-id
        :args (s/cat :account_id :account.feed/account_id :feed_id :account.feed/feed_id)
        :ret ::model)

;; insert

(defn insert [model]
  (log/info "insert" model)
  (let [affected-rows (jdbc/insert! db table model opts)]
    (log/info "affected rows" affected-rows)
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause   :account-data-feed-insert
                       :reason  :no-rows-affected
                       :details [db table model]}))
      (first affected-rows))))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [{:account.feed/keys [id]}]
  (log/info "delete" id)
  (let [affected-rows (jdbc/delete! db table ["id = ?", id] opts)]
    (log/info "affected rows" affected-rows)
    affected-rows))

(s/fdef delete
        :args (s/cat :id :account.feed/id)
        :ret int?)
