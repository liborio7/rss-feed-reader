(ns rss-feed-reader.domain.feed.item.pruner
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed-item :as feed-item]
            [rss-feed-reader.domain.job.scheduler :as job-scheduler]
            [com.stuartsierra.component :as component])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn handler [feeds-items]
  (fn [_payload]
    (log/trace "prune old feed items")
    (let [instant (-> (Instant/now)
                      (.minus 5 ChronoUnit/DAYS))
          feed-items-count (feed-item/delete-older-than! feeds-items instant)]
      {::old-feed-items-count feed-items-count})))

;; component

(defrecord FeedsItemsPruner [config
                             job-scheduler feeds-items
                             job]
  component/Lifecycle
  (start [this]
    (log/info "start pruner")
    (if job
      this
      (let [model {:job.domain/name        "feed-items-pruner"
                   :job.domain/description "Prune old feed items"}]
        (assoc this :job (job-scheduler/schedule job-scheduler config model (handler feeds-items))))))
  (stop [this]
    (log/info "stop pruner")
    (if job
      (do
        (job-scheduler/cancel job-scheduler job)
        (assoc this :job nil))
      this)))