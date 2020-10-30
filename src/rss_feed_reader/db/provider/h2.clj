(ns rss-feed-reader.db.provider.h2
  (:require [rss-feed-reader.db.datasource]
            [rss-feed-reader.db.migration :as migration]
            [hikari-cp.core :as hikari]
            [com.stuartsierra.component :as component]
            [next.jdbc.result-set :as rs]
            [next.jdbc.prepare :as p]
            [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import (java.sql PreparedStatement Blob)
           (clojure.lang IPersistentMap)
           (java.io ByteArrayInputStream)))

(defn ->blob [x]
  (ByteArrayInputStream. (json/generate-cbor x)))

(defn <-blob [^Blob v]
  (->> (.getBytes v 0 (.length v))
       (json/parse-cbor)
       (map (fn [[k v]] [(keyword k) v]))
       (into {})))

(extend-protocol p/SettableParameter

  IPersistentMap
  (set-parameter [^IPersistentMap v ^PreparedStatement ps ^long i]
    (.setBlob ps i ^ByteArrayInputStream (->blob v))))

(extend-protocol rs/ReadableColumn

  Blob
  (read-column-by-label [^Blob v _]
    (<-blob v))
  (read-column-by-index [^Blob v _2 _3]
    (<-blob v)))

;; component

(defn h2-config [config]
  {:url     (:h2-jdbc config)
   :adapter "h2"})

(defrecord H2Datasource [config on-start on-stop
                         datasource]
  component/Lifecycle
  (start [this]
    (log/info "start h2")
    (if datasource
      this
      (let [config (h2-config config)
            datasource (hikari/make-datasource config)
            this (assoc this :datasource datasource)]
        (when on-start (on-start this))
        this)))
  (stop [this]
    (log/info "stop h2")
    (if datasource
      (do
        (when on-stop (on-stop this))
        (hikari/close-datasource datasource)
        (assoc this :datasource nil))
      this))

  migration/Migrate
  (migrate [_]
    (migration/do-migrate datasource "h2"))
  (rollback [_]
    (migration/do-rollback datasource "h2")))
