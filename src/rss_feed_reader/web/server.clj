(ns rss-feed-reader.web.server
  (:require [rss-feed-reader.web.router :as router]
            [ring.adapter.jetty :as jetty]
            [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]))

(defrecord WebServer [config
                      accounts accounts-feeds feeds feeds-items
                      web-server]
  component/Lifecycle
  (start [this]
    (log/info "start web server")
    (if web-server
      this
      (let [port (Integer/parseInt (:port config))
            handler (router/handler accounts accounts-feeds feeds feeds-items)
            webserver (jetty/run-jetty handler {:port  port
                                                :join? false})]
        (assoc this :web-server webserver))))
  (stop [this]
    (log/info "stop web server")
    (if web-server
      (do
        (.stop web-server)
        (.join web-server)
        (assoc this :web-server nil))
      this)))