(ns rss-feed-reader.db.provider.postgres
  (:require [rss-feed-reader.db.datasource]
            [next.jdbc.result-set :as rs]
            [next.jdbc.prepare :as p]
            [cheshire.core :as json]
            [com.stuartsierra.component :as component]
            [hikari-cp.core :as hikari]
            [rss-feed-reader.db.migration :as migration]
            [clojure.tools.logging :as log])
  (:import (org.postgresql.util PGobject)
           (java.sql PreparedStatement)
           (clojure.lang IPersistentMap)))

(defn ->pg-object [x]
  (let [pg-type (or (:pgtype (meta x)) "jsonb")]
    (doto (PGobject.)
      (.setType pg-type)
      (.setValue (json/generate-string x)))))

(defn <-pg-object [^PGobject v]
  (let [type (.getType v)
        value (.getValue v)]
    (if (#{"jsonb" "json"} type)
      (let [value (->> (json/parse-string value)
                       (map (fn [[k v]] [(keyword k) v]))
                       (into {}))]
        (with-meta value {:pgtype type}))
      value)))

(extend-protocol p/SettableParameter

  IPersistentMap
  (set-parameter [^IPersistentMap v ^PreparedStatement ps ^long i]
    (.setObject ps i (->pg-object v))))

(extend-protocol rs/ReadableColumn

  PGobject
  (read-column-by-label [^PGobject v _]
    (<-pg-object v))
  (read-column-by-index [^PGobject v _2 _3]
    (<-pg-object v)))

(defn postgres-config [config]
  (merge {:username      (:postgres-user config)
          :password      (:postgres-password config)
          :database-name (:postgres-db config)
          :server-name   (:postgres-host config)
          :port-number   (:postgres-port config)}
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
          :register-mbeans    false}))

;; component

(defrecord PostgresDatasource [config
                               datasource]
  component/Lifecycle
  (start [this]
    (log/info "start postgres")
    (if datasource
      this
      (assoc this :datasource (hikari/make-datasource (postgres-config config)))))
  (stop [this]
    (log/info "stop postgres")
    (if datasource
      (do
        (hikari/close-datasource datasource)
        (assoc this :datasource nil
                    :migration-path nil))
      this))

  migration/Migrate
  (migrate [_]
    (migration/do-migrate datasource "postgres"))
  (rollback [_]
    (migration/do-rollback datasource "postgres")))
