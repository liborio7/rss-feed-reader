(ns rss-feed-reader.utils.sql
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as q]))

(defn default-opts [table]
  {:qualifier (clojure.string/replace (name table) "_" ".")})

(defn get-by-id
  ([db table id-keyword model] (get-by-id db table id-keyword model {}))
  ([db table id-keyword model opts]
   (log/info "select from" table id-keyword model)
   (let [query (-> (q/build :select :*
                            :from table
                            :where [:= id-keyword (id-keyword model)])
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/info query "returns" result)
     (if (> 1 (count result))
       (log/warn "unexpected multiple results"))
     (first result))))

(defn get-by-query
  ([db table clause] (get-by-query db table clause {}))
  ([db table clause opts]
   (log/info "select from" table clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/info query "returns" result)
     (if (> 1 (count result))
       (log/warn "unexpected multiple results"))
     (first result))))

(defn get-by-query-multi
  ([db table clause] (get-by-query-multi db table clause {}))
  ([db table clause opts]
   (log/info "select multiple from" table clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/info query "returns" (count result) "results")
     result)))

(defn insert
  ([db table id-keyword model] (insert db table id-keyword model {}))
  ([db table id-keyword model opts]
   (log/info "insert into" table id-keyword model)
   (let [query (-> (q/build :insert-into table
                            :values [model])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/info query "affects" affected-rows "row(s)")
     (if (empty? affected-rows)
       (throw (ex-info "no rows has been inserted"
                       {:cause   :sql-insert
                        :reason  :no-rows-affected
                        :details [db table model]})))
     (if (> 1 (count affected-rows))
       (log/warn "unexpected multiple results"))
     (get-by-id db table id-keyword model))))


(defn delete
  ([db table id-keyword model] (delete db table id-keyword model {}))
  ([db table id-keyword model opts]
   (log/info "delete from" table id-keyword model)
   (let [query (-> (q/build :delete-from table
                            :where [:= id-keyword (id-keyword model)])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/info query "affects" affected-rows "row(s)")
     (if (> 1 (count affected-rows))
       (log/warn "unexpected multiple results"))
     affected-rows)))