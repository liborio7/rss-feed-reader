(ns rss-feed-reader.utils.sql
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.walk :refer [walk]]
            [clojure.string :as str]
            [honeysql.core :as q]
            [honeysql.format :as qf]))

(defn- default-opts [table]
  {:qualifier (str/replace (name table) "_" ".")})

(defn- format-values [m]
  (walk
    (fn [[k v]] [k (qf/value v)])
    identity
    m))

;; select

(defn get-multi-by-id
  ([ds table id-keyword models] (get-multi-by-id ds table id-keyword models {}))
  ([ds table id-keyword models opts]
   (log/trace "get by id" table id-keyword models)
   (let [ids (->> models
                  (map id-keyword))
         query (-> (q/build :select :*
                            :from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/with-db-connection [conn {:datasource ds}]
                                         (jdbc/query conn query opts))]
     (log/trace query "returns" result)
     result)))

(defn get-by-id
  ([ds table id-keyword model] (get-by-id ds table id-keyword model {}))
  ([ds table id-keyword model opts]
   (let [result (get-multi-by-id ds table id-keyword (conj [] model) opts)]
     (when (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

(defn get-multi-by-query
  ([ds table where-clause] (get-multi-by-query ds table where-clause {}))
  ([ds table where-clause opts]
   (log/trace "get by query" table where-clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/with-db-connection [conn {:datasource ds}]
                                         (jdbc/query conn query opts))]
     (log/trace query "returns" (count result) "results")
     result)))

(defn get-by-query
  ([ds table clause] (get-by-query ds table clause {}))
  ([ds table clause opts]
   (let [result (get-multi-by-query ds table clause opts)]
     (when (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; insert

(defn insert-multi
  ([ds table id-keyword models] (insert-multi ds table id-keyword models {}))
  ([ds table id-keyword models opts]
   (log/trace "insert " table id-keyword models)
   (let [values (->> models
                     (map format-values)
                     (reduce conj []))
         query (-> (q/build :insert-into table
                            :values values)
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/with-db-connection [conn {:datasource ds}]
                                                (jdbc/execute! conn query opts))]
     (log/trace query "affects" affected-rows "row(s)")
     (when (empty? affected-rows)
       (throw (ex-info "no rows has been inserted"
                       {:cause   :sql-insert
                        :reason  :no-rows-affected
                        :details [ds table models]})))
     (get-multi-by-id ds table id-keyword models))))

(defn insert
  ([ds table id-keyword model] (insert ds table id-keyword model {}))
  ([ds table id-keyword model opts]
   (let [result (insert-multi ds table id-keyword (conj [] model) opts)]
     (when (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; update

(defn update
  ([ds table id-keyword version-keyword model] (update ds table id-keyword version-keyword model {}))
  ([ds table id-keyword version-keyword model opts]
   (log/trace "update" table id-keyword model)
   (let [values (->> (update-in model [version-keyword] inc)
                     (filter not-empty)
                     (format-values)
                     (into {}))
         query (-> (q/build :update table
                            :set values
                            :where [:and
                                    [:= id-keyword (id-keyword model)]
                                    [:= version-keyword (version-keyword model)]])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/with-db-connection [conn {:datasource ds}]
                                                (jdbc/execute! conn query opts))]
     (log/trace query "affects" affected-rows "row(s)")
     (when (empty? affected-rows)
       (throw (ex-info "no rows has been updated"
                       {:cause   :sql-update
                        :reason  :no-rows-affected
                        :details [ds table model]})))
     (get-by-id ds table id-keyword model opts))))

;; delete

(defn delete-multi
  ([ds table id-keyword models] (delete-multi ds table id-keyword models {}))
  ([ds table id-keyword models opts]
   (log/trace "delete " table id-keyword models)
   (let [ids (->> models
                  (map id-keyword))
         query (-> (q/build :delete-from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/with-db-connection [conn {:datasource ds}]
                                                (jdbc/execute! conn query opts))]
     (log/trace query "affects" affected-rows "row(s)")
     affected-rows)))

(defn delete
  ([ds table id-keyword model] (delete ds table id-keyword model {}))
  ([ds table id-keyword model opts]
   (let [affected-rows (delete-multi ds table id-keyword (conj [] model) opts)]
     (when (> affected-rows 1)
       (log/warn "unexpected multiple results"))
     affected-rows)))