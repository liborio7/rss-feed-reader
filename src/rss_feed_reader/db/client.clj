(ns rss-feed-reader.db.client
  (:refer-clojure :exclude [update])
  (:require [rss-feed-reader.db.datasource :refer [ds]]
            [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [clojure.walk :refer [walk]]
            [clojure.string :as str]
            [honeysql.core :as q]
            [honeysql.format :as qf]))

(defn default-opts [table]
  {:qualifier (str/replace (name table) "_" ".")})

(defn format-values [m]
  (walk
    (fn [[k v]] [k (qf/value v)])
    identity
    m))

;; execute

(defn db-query [stm opts]
  (jdbc/with-db-connection [conn {:datasource ds}]
                           (jdbc/query conn stm opts)))

(defn db-execute! [stm opts]
  (jdbc/with-db-connection [conn {:datasource ds}]
                           (jdbc/execute! conn stm opts)))

;; pagination

(defn paginate-select
  ([select-fn sa-fn] (paginate-select select-fn sa-fn 0))
  ([select-fn sa-fn initial-sa]
   (sequence
     (comp
       (take-while seq)
       cat)
     (iterate
       (fn [coll]
         (when-not (empty? coll)
           (let [sa (sa-fn (last coll))]
             (select-fn sa))))
       (select-fn initial-sa)))))

;; select

(defn select-values
  ([table where-clause] (select-values table where-clause {}))
  ([table where-clause opts]
   (log/debug "select from" table where-clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         db-result (db-query query opts)]
     (log/trace query "returns" (count db-result) "results")
     db-result)))

(defn select
  ([table clause] (select table clause {}))
  ([table clause opts]
   (let [result (select-values table clause opts)]
     (when (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; insert

(defn insert-values!
  ([table models] (insert-values! table models {}))
  ([table models opts]
   (log/debug "insert into" table models)
   (let [values (->> (vec models)
                     (map format-values)
                     (reduce conj []))
         query (-> (q/build :insert-into table
                            :values values)
                   (q/format))
         opts (merge (default-opts table) opts)
         db-result (db-execute! query opts)
         affected-rows (first db-result)]
     (log/trace query "affects" affected-rows "row(s)")
     (when (zero? affected-rows)
       (throw (ex-info "no rows has been inserted"
                       {:cause   :sql-insert
                        :reason  :no-rows-affected
                        :details [query]})))
     (when (< affected-rows (count models))
       (log/warn "expected to affect" (count models) "rows but was" affected-rows))
     models)))

(defn insert!
  ([table model] (insert! table model {}))
  ([table model opts]
   (let [sql-result (insert-values! table (conj [] model) opts)]
     (when (> (count sql-result) 1)
       (log/warn "unexpected multiple results"))
     (first sql-result))))

;; update

(defn update-values!
  ([table where-clause model] (update-values! table where-clause model {}))
  ([table where-clause model opts]
   (log/debug "update" table model)
   (let [query (-> (q/build :update table
                            :set (format-values model))
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         db-result (db-execute! query opts)
         affected-rows (first db-result)]
     (log/trace query "affects" affected-rows "row(s)")
     (when (zero? affected-rows)
       (throw (ex-info "no rows has been updated"
                       {:cause   :sql-update
                        :reason  :no-rows-affected
                        :details [query]})))
     affected-rows)))

(defn update!
  ([table where-clause model] (update! table where-clause model {}))
  ([table where-clause model opts]
   (log/debug "update" table model)
   (let [query (-> (q/build :update table
                            :set (format-values model))
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         db-result (db-execute! query opts)
         affected-rows (first db-result)]
     (log/trace query "affects" affected-rows "row(s)")
     (when (zero? affected-rows)
       (throw (ex-info "no rows has been updated"
                       {:cause   :sql-update
                        :reason  :no-rows-affected
                        :details [query]})))
     (when (> affected-rows 1)
       (log/warn "unexpected multiple results"))
     model)))

;; delete

(defn delete!
  ([table where-clause] (delete! table where-clause {}))
  ([table where-clause opts]
   (log/debug "delete from" table where-clause)
   (let [query (-> (q/build :delete-from table)
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         db-result (db-execute! query opts)
         affected-rows (first db-result)]
     (log/trace query "affects" affected-rows "row(s)")
     affected-rows)))