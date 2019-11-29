(ns rss-feed-reader.postgres.migrations
  (:require [ragtime.jdbc :as jdbc]
            [rss-feed-reader.postgres.db :as db]))

(def spec
  {:datastore  (jdbc/sql-database db/connection)
   :migrations (jdbc/load-resources "migrations")})
