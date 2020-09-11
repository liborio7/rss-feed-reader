(ns user
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount :refer [defstate]]
            [mount.tools.graph :refer [states-with-deps]]))

(def postgres-ds "#'rss-feed-reader.db.datasource/ds")
(def app-webserver "#'rss-feed-reader.app/webserver")
(def atat-pool "#'rss-feed-reader.scheduler.atat/pool")

(def core-states
  [postgres-ds app-webserver atat-pool])

(def bot-job "#'rss-feed-reader.bot.job/job")
(def feeder-job "#'rss-feed-reader.rss.feeder/job")

(def job-states
  [bot-job feeder-job])

(defn start
  ([] (start core-states))
  ([& states] (apply mount/start states)))

(defn stop
  ([] (mount/stop))
  ([& states] (apply mount/stop states)))

(defn restart []
  (stop)
  (start))

(defn refresh []
  (stop)
  (tn/refresh))

(defn refresh-all []
  (stop)
  (tn/refresh-all))

(defn go
  "starts all states defined by defstate"
  []
  (start)
  :ready)

(defn reset
  "stops all states defined by defstate, reloads modified source files, and restarts the states"
  []
  (stop)
  (tn/refresh :after 'user/go))

(defn status
  "shows current mount state with dependencies"
  []
  (states-with-deps))

(mount/in-clj-mode)