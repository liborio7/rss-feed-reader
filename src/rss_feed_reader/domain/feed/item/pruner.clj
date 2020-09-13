(ns rss-feed-reader.domain.feed.item.pruner
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed-item :as feed-items]
            [mount.core :refer [defstate]]
            [rss-feed-reader.scheduler.executor :refer [start stop]])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn prune
  ([] (prune {}))
  ([_]
   (log/trace "prune old feed items")
   (let [instant (-> (Instant/now)
                     (.minus 3 ChronoUnit/DAYS))
         feed-items-count (feed-items/delete-older-than! instant)]
     {::old-feed-items-count feed-items-count})))

(defn start-job []
  (let [job-model {:job.domain/name        "feed-items-pruner"
                   :job.domain/description "Prune old feed items"}]
    (log/info "start pruner job")
    (start :scheduler-feed-items-pruner job-model prune)))

(defn stop-job [job]
  (log/info "stop feeder job")
  (stop job))

(defstate job
  :start (start-job)
  :stop (stop-job job))

