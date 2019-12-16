(ns rss-feed-reader.data.feed
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [honeysql.core :as sql]))

;; utils

(def db db/connection)
(def table :feed)
(def opts {:qualifier (clojure.string/replace (name table) "_" ".")})

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

(defn get-by-id [{:feed/keys [id]}]
  (log/info "get by id" id)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :feed/id id])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-id
        :args (s/cat :id :feed/id)
        :ret ::model)

(defn get-by-link [{:feed/keys [link]}]
  (log/info "get by link" link)
  (let [query (-> (sql/build :select :*
                             :from table
                             :where [:= :feed/link link])
                  (sql/format))
        result (jdbc/query db query opts)]
    (log/info query "returns" result)
    (if (> 1 (count result))
      (log/warn "unexpected multiple results"))
    (first result)))

(s/fdef get-by-link
        :args (s/cat :link :feed/link)
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
                      {:cause   :feed-data-insert
                       :reason  :no-rows-affected
                       :details [db table model]})))
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    (get-by-id model)))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [{:feed/keys [id]}]
  (log/info "delete" id)
  (let [query (-> (sql/build :delete-from table
                             :where [:= :feed/id id])
                  (sql/format))
        affected-rows (jdbc/execute! db query opts)]
    (log/info query "affects" affected-rows "row(s)")
    (if (> 1 (count affected-rows))
      (log/warn "unexpected multiple results"))
    affected-rows))

(s/fdef delete
        :args (s/cat :id :feed/id)
        :ret (s/and int?))