(ns rss-feed-reader.utils.sql
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [clojure.java.jdbc :as jdbc]
            [honeysql.core :as q]))

(defn- default-opts [table]
  {:qualifier (clojure.string/replace (name table) "_" ".")})

;; select

(defn get-multi-by-id
  ([db table id-keyword models] (get-multi-by-id db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/trace "get by id" table id-keyword models)
   (let [ids (->> models
                  (map id-keyword)
                  (reduce conj []))
         query (-> (q/build :select :*
                            :from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/trace query "returns" result)
     result)))

(defn get-by-id
  ([db table id-keyword model] (get-by-id db table id-keyword model {}))
  ([db table id-keyword model opts]
   (let [result (get-multi-by-id db table id-keyword (conj [] model) opts)]
     (if (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

(defn get-multi-by-query
  ([db table where-clause] (get-multi-by-query db table where-clause {}))
  ([db table where-clause opts]
   (log/trace "get by query" table where-clause)
   (let [query (-> (q/build :select :*
                            :from table)
                   (merge where-clause)
                   (q/format))
         opts (merge (default-opts table) opts)
         result (jdbc/query db query opts)]
     (log/trace query "returns" (count result) "results")
     result)))

(defn get-by-query
  ([db table clause] (get-by-query db table clause {}))
  ([db table clause opts]
   (let [result (get-multi-by-query db table clause opts)]
     (if (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; insert

(defn insert-multi
  ([db table id-keyword models] (insert-multi db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/trace "insert " table id-keyword models)
   (let [query (-> (q/build :insert-into table
                            :values (apply conj [] models))
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/trace query "affects" affected-rows "row(s)")
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
     (if (> (count result) 1)
       (log/warn "unexpected multiple results"))
     (first result))))

;; update

(defn update
  ([db table id-keyword version-keyword model] (update db table id-keyword version-keyword model {}))
  ([db table id-keyword version-keyword model opts]
   (log/trace "update" table id-keyword model)
   (let [model-skip-null (->> model
                              (filter #(not (nil? (second %))))
                              (into {}))
         query (-> (q/build :update table
                            :set (update-in model-skip-null [version-keyword] inc)
                            :where [:and
                                    [:= id-keyword (id-keyword model)]
                                    [:= version-keyword (version-keyword model)]])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/trace query "affects" affected-rows "row(s)")
     (if (empty? affected-rows)
       (throw (ex-info "no rows has been updated"
                       {:cause   :sql-update
                        :reason  :no-rows-affected
                        :details [db table model]})))
     (get-by-id db table id-keyword model opts))))

;; delete

(defn delete-multi
  ([db table id-keyword models] (delete-multi db table id-keyword models {}))
  ([db table id-keyword models opts]
   (log/trace "delete " table id-keyword models)
   (let [ids (->> models
                  (map id-keyword)
                  (reduce conj []))
         query (-> (q/build :delete-from table
                            :where [:in id-keyword ids])
                   (q/format))
         opts (merge (default-opts table) opts)
         affected-rows (jdbc/execute! db query opts)]
     (log/trace query "affects" affected-rows "row(s)")
     affected-rows)))

(defn delete
  ([db table id-keyword model] (delete db table id-keyword model {}))
  ([db table id-keyword model opts]
   (let [affected-rows (delete-multi db table id-keyword (conj [] model) opts)]
     (if (> 1 affected-rows)
       (log/warn "unexpected multiple results"))
     affected-rows)))