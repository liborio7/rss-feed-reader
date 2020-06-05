(ns rss-feed-reader.app
  (:require [rss-feed-reader.jobs]
            [rss-feed-reader.bot.job]
            [rss-feed-reader.env :refer [env]]
            [ring.adapter.jetty :as jetty]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.api.router :refer [handler]]))

(defn -main [& _args]
  (cid/set-new)
  (log/info "environment:" (:environment env)) +
  (jetty/run-jetty handler {:port 3000}))