(ns rss-feed-reader.db.postgres
  (:require [clj-time.jdbc]
            [rss-feed-reader.env :refer [env]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

(def connection
  {:dbtype   "postgresql"
   :dbname   (:postgres_database env)
   :host     (:postgres_host env)
   :user     (:postgres_user env)
   :password (:postgres_password env)})

(def ragtime-config
  {:datastore  (jdbc/sql-database connection)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate ragtime-config))

(defn rollback []
  (repl/rollback ragtime-config))