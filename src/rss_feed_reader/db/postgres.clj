(ns rss-feed-reader.db.postgres
  (:require [clj-time.jdbc]
            [clojure.java.jdbc :refer [ISQLValue IResultSetReadColumn]]
            [hikari-cp.core :as hikari]
            [cheshire.core :as json]
            [rss-feed-reader.env :refer [env]]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl])
  (:import (org.postgresql.util PGobject)
           (clojure.lang IPersistentMap)))

(extend-protocol ISQLValue
  IPersistentMap
  (sql-value [value] (doto (PGobject.)
                       (.setType "jsonb")
                       (.setValue (json/generate-string value)))))

(extend-protocol IResultSetReadColumn
  PGobject
  (result-set-read-column [pg-obj _ _]
    (let [type (.getType pg-obj)
          value (.getValue pg-obj)]
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
                   :port-number        (:postgres-port env)
                   :register-mbeans    false})))

(def ragtime-config
  {:datastore  (jdbc/sql-database {:datasource @datasource})
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate ragtime-config))

(defn rollback []
  (repl/rollback ragtime-config))