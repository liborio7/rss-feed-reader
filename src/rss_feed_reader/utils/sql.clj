(ns rss-feed-reader.utils.sql
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as q]))

(defn default-opts [table]
  {:qualifier (clojure.string/replace (name table) "_" ".")})

;; select

(defn get-multi-by-id
  ([db table id-keyword models] (get-multi-by-id db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/debug "get by id multi" table id-keyword models)
   (let [ids (->> models
                  (map id-keyword)
                  (apply conj []))
         query (-> (q/build :select :*
                            :from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/debug query "returns" result)
     result)))

(defn get-by-id
  ([db table id-keyword model] (get-by-id db table id-keyword model {}))
  ([db table id-keyword model opts]
   (let [result (get-multi-by-id db table id-keyword (conj [] model) opts)]
     (if (> 1 (count result))
       (log/warn "unexpected multiple results"))
     (first result))))

(defn get-multi-by-query
  ([db table clause] (get-multi-by-query db table clause {}))
  ([db table clause opts]
   (log/debug "get by query" table clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/debug query "returns" (count result) "results")
     result)))

(defn get-by-query
  ([db table clause] (get-by-query db table clause {}))
  ([db table clause opts]
   (let [result (get-multi-by-query db table clause opts)]
     (if (> 1 (count result))
       (log/warn "unexpected multiple results"))
     (first result))))

;; insert

(defn insert-multi
  ([db table id-keyword models] (insert-multi db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/debug "insert " table id-keyword models)
   (let [query (-> (q/build :insert-into table
                            :values (apply conj [] models))
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/debug query "affects" affected-rows "row(s)")
     (if (empty? affected-rows)
       (throw (ex-info "no rows has been inserted"
                       {:cause   :sql-insert
                        :reason  :no-rows-affected
                        :details [db table models]})))
     (get-multi-by-id db table id-keyword models))))

(defn insert
  ([db table id-keyword model] (insert db table id-keyword model {}))
  ([db table id-keyword model opts]
   (let [result (insert-multi db table id-keyword (conj [] model) opts)]
     (if (> 1 (count result))
       (log/warn "unexpected multiple results"))
     (first result))))

;; delete

(defn delete-multi
  ([db table id-keyword models] (delete-multi db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/debug "delete " table id-keyword models)
   (let [ids (->> models
                  (map id-keyword)
                  (apply conj []))
         query (-> (q/build :delete-from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/debug query "affects" affected-rows "row(s)")
     affected-rows)))

(defn delete
  ([db table id-keyword model] (delete db table id-keyword model {}))
  ([db table id-keyword model opts]
   (let [affected-rows (delete-multi db table id-keyword (conj [] model) opts)]
     (if (> 1 affected-rows)
       (log/warn "unexpected multiple results"))
     affected-rows)))