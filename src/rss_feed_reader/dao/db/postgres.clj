(ns rss-feed-reader.dao.db.postgres
  (:require [environ.core :refer [env]]
            [clojure.edn :as edn]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]))

(def connection
  (let [props (->> (:environment env)
                   (format "resources/%s/db.edn")
                   (slurp)
                   (edn/read-string))]
    {:dbtype   "postgresql"
     :dbname   (:database props)
     :host     (:host props)
     :user     (:user props)
     :password (:password props)}))

(def ragtime-config
  {:datastore  (jdbc/sql-database connection)
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate ragtime-config))

(defn rollback []
  (repl/rollback ragtime-config))