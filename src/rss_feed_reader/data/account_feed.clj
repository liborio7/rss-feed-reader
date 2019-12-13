(ns rss-feed-reader.data.account_feed
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.spec.alpha :as s]
            [clojure.java.jdbc :as jdbc]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]))

;; utils

(def db db/connection)
(def table :account_feed)
(def opts {:qualifier (clojure.string/replace (name table) "_" ".")})

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
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :account.feed/id id])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-id
        :args (s/cat :id :account.feed/id)
        :ret ::model)

(defn get-by-account-id [{:account.feed/keys [account_id]}]
  (log/info "get by account id" account_id)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :account.feed/account_id account_id])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" (count result) "results")
    result))

(s/fdef get-by-account-id
        :args (s/cat :account_id :account.feed/account_id)
        :ret (s/coll-of ::model))

(defn get-by-feed-id [{:account.feed/keys [feed_id]}]
  (log/info "get by feed id" feed_id)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :account.feed/feed_id feed_id])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" (count result) "results")
    result))

(s/fdef get-by-feed-id
        :args (s/cat :feed_id :account.feed/feed_id)
        :ret (s/coll-of ::model))

(defn get-by-account-id-and-feed-id [{:account.feed/keys [account_id feed_id]}]
  (log/info "get by account id and feed id" account_id feed_id)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:and [:= :account.feed/account_id account_id] [:= :account.feed/feed_id feed_id]])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-account-id-and-feed-id
        :args (s/cat :account_id :account.feed/account_id :feed_id :account.feed/feed_id)
        :ret ::model)

;; insert

(defn insert [model]
  (log/info "insert" model)

  (let [query (-> (sql/build :insert-into table
                             :values [model])
                  (sql/format))
        affected-rows (jdbc/execute! db query opts)]
    (log/info query "affects" affected-rows "row(s)")
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause   :account-feed-data-insert
                       :reason  :no-rows-affected
                       :details [db table model]})))
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    (get-by-id model)))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [{:account.feed/keys [id]}]
  (log/info "delete" id)
  (let [query (-> (sql/build :delete-from table
                             :where [:= :account.feed/id id])
                  (sql/format))
        affected-rows (jdbc/execute! db query opts)]
    (log/info query "affects" affected-rows "row(s)")
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    affected-rows))

(s/fdef delete
        :args (s/cat :id :account.feed/id)
        :ret int?)
