(ns rss-feed-reader.app
  (:gen-class)
  (:require [mount.core :as mount :refer [defstate]]
            [rss-feed-reader.env :refer [env]]
            [rss-feed-reader.db.datasource :refer [ds]]
            [rss-feed-reader.scheduler.executor :refer [pool]]
            [rss-feed-reader.bot.updater]
            [rss-feed-reader.rss.feeder]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.api.router :refer [handler]]))

(defn start-webserver [env]
  (let [port (->> (or (:port env) "3000")
                  (Integer/parseInt))]
    (log/info "start web server")
    (jetty/run-jetty handler {:port  port
                              :join? false})))

(defn stop-webserver [webserver]
  (log/info "stop web server")
  (.stop webserver))

(defstate webserver
  :start (start-webserver env)
  :stop (stop-webserver webserver))

(defn -main [& _args]
  (cid/set-new)
  (mount/start))