(ns rss-feed-reader.db.migration
  (:require [ragtime.repl :as ragtime-repl]
            [ragtime.jdbc :as ragtime-jdbc]))

(defn ragtime-config [datasource path]
  {:datastore  (ragtime-jdbc/sql-database {:datasource datasource})
   :migrations (ragtime-jdbc/load-resources (format "migrations/%s" path))})

(defn do-migrate [datasource path]
  (ragtime-repl/migrate (ragtime-config datasource path)))

(defn do-rollback [datasource path]
  (ragtime-repl/rollback (ragtime-config datasource path) Integer/MAX_VALUE))

(defprotocol Migrate
  (migrate [this])
  (rollback [this]))
