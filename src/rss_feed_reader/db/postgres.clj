(ns rss-feed-reader.db.postgres
  (:require [clj-time.jdbc]
            [mount.core :refer [defstate]]
            [rss-feed-reader.env :refer [env]]
            [clojure.java.jdbc :refer [ISQLValue ISQLParameter IResultSetReadColumn]]
            [hikari-cp.core :as hikari]
            [cheshire.core :as json]
            [ragtime.jdbc :as jdbc]
            [ragtime.repl :as repl]
            [clojure.tools.logging :as log])
  (:import (org.postgresql.util PGobject)
           (clojure.lang IPersistentMap)
           (java.sql PreparedStatement Timestamp)
           (java.time Instant)))

(extend-protocol ISQLParameter
  Instant
  (set-parameter [value ^PreparedStatement ps ^long idx]
    (.setObject ps idx (Timestamp/from value))))

(extend-protocol IResultSetReadColumn
  Timestamp
  (result-set-read-column [d _ _]
    (.toInstant d)))

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

(defn config
  ([] (config {:username      (:postgres-user env)
               :password      (:postgres-password env)
               :database-name (:postgres-db env)
               :server-name   (:postgres-host env)
               :port-number   (:postgres-port env)}))
  ([jdbc-config] (merge jdbc-config
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
                         :register-mbeans    false})))


(defn start-ds
  ([] (start-ds (config)))
  ([config]
   (log/info "start ds")
   (hikari/make-datasource config)))

(defn stop-ds [ds]
  (log/info "stop ds")
  (hikari/close-datasource ds))

(defstate ds
  :start (start-ds)
  :stop (stop-ds ds))

(defn ragtime-config
  ([] (ragtime-config (start-ds)))
  ([ds] {:datastore  (jdbc/sql-database {:datasource ds})
         :migrations (jdbc/load-resources "migrations")}))

(defn migrate
  ([]
   (with-open [ds (start-ds)]
     (migrate (ragtime-config ds))))
  ([ragtime-config]
   (repl/migrate ragtime-config)))

(defn rollback
  ([]
   (with-open [ds (start-ds)]
     (rollback (ragtime-config ds))))
  ([ragtime-config]
   (repl/rollback ragtime-config)))