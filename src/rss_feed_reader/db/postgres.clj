(ns rss-feed-reader.db.postgres
  (:require [clj-time.jdbc]
            [hikari-cp.core :as hikari]
            [cheshire.core :as json]
            [rss-feed-reader.env :refer [env]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl])
  (:import (org.postgresql.util PGobject)))

(extend-protocol clojure.java.jdbc/ISQLValue
  clojure.lang.IPersistentMap
  (sql-value [value] (doto (PGobject.)
                       (.setType "jsonb")
                       (.setValue (cheshire.core/generate-string value)))))

(extend-protocol clojure.java.jdbc/IResultSetReadColumn
  org.postgresql.util.PGobject
  (result-set-read-column [pgobj _ _]
    (let [type (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/parse-string value true)
        "jsonb" (json/parse-string value true)
        value))))

(defonce datasource
         (delay (hikari/make-datasource
                  {:auto-commit        true
                   :read-only          false
                   :connection-timeout 30000
                   :validation-timeout 5000
                   :idle-timeout       600000
                   :max-lifetime       1800000
                   :minimum-idle       10
                   :maximum-pool-size  10
                   :pool-name          "db-pool"
                   :adapter            "postgresql"
                   :username           (:postgres-user env)
                   :password           (:postgres-password env)
                   :database-name      (:postgres-db env)
                   :server-name        (:postgres-host env)
                   :port-number        5432
                   :register-mbeans    false})))

(def ragtime-config
  {:datastore  (jdbc/sql-database {:datasource @datasource})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate ragtime-config))

(defn rollback []
  (repl/rollback ragtime-config))