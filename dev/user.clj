(ns user
  (:require [clojure.pprint :refer [pprint]]
            [clojure.tools.namespace.repl :as tn]
            [mount.core :as mount :refer [defstate]]
            [mount.tools.graph :refer [states-with-deps]]))

(def app-webserver "#'rss-feed-reader.app/webserver")
(def postgres-ds "#'rss-feed-reader.db.datasource/ds")
(def atat-pool "#'rss-feed-reader.scheduler.executor/pool")

(def core-states
  [postgres-ds app-webserver atat-pool])

(def bot-updater "#'rss-feed-reader.bot.updater/job")
(def rss-feeder "#'rss-feed-reader.rss.feeder/job")
(def feed-items-pruner "#'rss-feed-reader.domain.feed.item.pruner/job")

(def job-states
  [bot-updater rss-feeder feed-items-pruner])

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