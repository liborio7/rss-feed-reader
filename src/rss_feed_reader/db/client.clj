(ns rss-feed-reader.db.client
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [clojure.walk :refer [walk]]
            [clojure.string :as str]
            [honeysql.core :as sql]
            [honeysql.format :as sql-format]))

(defn builder-fn [rs opts]
  (let [kebab #(str/replace % #"_" "-")
        dot #(str/replace % #"_" ".")
        lower #(str/lower-case %)]
    (result-set/as-modified-maps rs (assoc opts
                                      :qualifier-fn (comp lower dot)
                                      :label-fn (comp lower kebab)))))

(defn default-opts []
  {:builder-fn builder-fn})

(defn format-values [m]
  (walk
    (fn [[k v]] [k (sql-format/value v)])
    identity
    m))

;; jdbc

(defn db-execute! [connectable query opts]
  (jdbc/execute! connectable query opts))

(defn db-execute-one! [connectable query opts]
  (jdbc/execute-one! connectable query opts))

(defmacro with-connection [[sym connectable] & body]
  `(with-open [~sym (jdbc/get-connection (:datasource ~connectable))]
     ~@body))

(defmacro with-transaction [[sym transactable] & body]
  `(jdbc/with-transaction [~sym (:datasource ~transactable)]
                          ~@body))

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
  ([connectable table where-clause] (select-values connectable table where-clause {}))
  ([connectable table where-clause opts]
   (log/debug "select from" table where-clause)
   (let [query (-> (sql/build :select :*
                              :from table)
                   (merge where-clause)
                   (sql/format))
         opts (merge (default-opts) opts)
         db-result (db-execute! connectable query opts)]
     (log/trace query "returns" (count db-result) "results")
     db-result)))

(defn select
  ([connectable table clause] (select connectable table clause {}))
  ([connectable table clause opts]
   (let [result (select-values connectable table clause opts)]
     (when (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; insert

(defn insert-values!
  ([connectable table models] (insert-values! connectable table models {}))
  ([connectable table models opts]
   (log/debug "insert into" table models)
   (let [values (->> (vec models)
                     (map format-values)
                     (reduce conj []))
         query (-> (sql/build :insert-into table
                              :values values)
                   (sql/format))
         opts (merge (default-opts) opts)
         db-result (db-execute! connectable query opts)
         affected-rows (:next.jdbc/update-count (first db-result))]
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
  ([connectable table model] (insert! connectable table model {}))
  ([connectable table model opts]
   (let [sql-result (insert-values! connectable table (conj [] model) opts)]
     (when (> (count sql-result) 1)
       (log/warn "unexpected multiple results"))
     (first sql-result))))

;; update

(defn update-values!
  ([connectable table where-clause model] (update-values! connectable table where-clause model {}))
  ([connectable table where-clause model opts]
   (log/debug "update" table model)
   (let [query (-> (sql/build :update table
                              :set (format-values model))
                   (merge where-clause)
                   (sql/format))
         opts (merge (default-opts) opts)
         db-result (db-execute! connectable query opts)
         affected-rows (:next.jdbc/update-count (first db-result))]
     (log/trace query "affects" affected-rows "row(s)")
     (when (zero? affected-rows)
       (throw (ex-info "no rows has been updated"
                       {:cause   :sql-update
                        :reason  :no-rows-affected
                        :details [query]})))
     affected-rows)))

(defn update!
  ([connectable table where-clause model] (update! connectable table where-clause model {}))
  ([connectable table where-clause model opts]
   (log/debug "update" table model)
   (let [query (-> (sql/build :update table
                              :set (format-values model))
                   (merge where-clause)
                   (sql/format))
         opts (merge (default-opts) opts)
         db-result (db-execute! connectable query opts)
         affected-rows (:next.jdbc/update-count (first db-result))]
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
  ([connectable table where-clause] (delete! connectable table where-clause {}))
  ([connectable table where-clause opts]
   (log/debug "delete from" table where-clause)
   (let [query (-> (sql/build :delete-from table)
                   (merge where-clause)
                   (sql/format))
         opts (merge (default-opts) opts)
         db-result (db-execute! connectable query opts)
         affected-rows (:next.jdbc/update-count (first db-result))]
     (log/trace query "affects" affected-rows "row(s)")
     affected-rows)))