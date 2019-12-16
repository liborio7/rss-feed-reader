(ns rss-feed-reader.data.account
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]))

;; utils

(def db db/connection)
(def table :account)
(def opts {:qualifier (clojure.string/replace (name table) "_" ".")})

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

;; get by id

(defn get-by-id [{:account/keys [id]}]
  (log/info "get by id" id)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :account/id id])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-id
        :args (s/cat :id :account/id)
        :ret ::model)

(defn get-by-username [{:account/keys [username]}]
  (log/info "get by username" username)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :account/username username])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-username
        :args (s/cat :username :account/username)
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
                      {:cause   :account-data-insert
                       :reason  :no-rows-affected
                       :details [db table model]})))
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    (get-by-id model)))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [{:account/keys [id]}]
  (log/info "delete" id)
  (let [query (-> (sql/build :delete-from table
                             :where [:= :account/id id])
                  (sql/format))
        affected-rows (jdbc/execute! db query opts)]
    (log/info query "affects" affected-rows "row(s)")
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    affected-rows))

(s/fdef delete
        :args (s/cat :id :account/id)
        :ret int?)