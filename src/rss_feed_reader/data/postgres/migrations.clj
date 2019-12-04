(ns rss-feed-reader.data.postgres.migrations
  (:require [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [rss-feed-reader.data.postgres.db :as db]))

(defn config []
  {:datastore  (jdbc/sql-database db/connection)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate (config)))

(defn rollback []
  (repl/rollback (config)))