(ns rss-feed-reader.jobs
  (:require [rss-feed-reader.env :refer [env]]
            [overtone.at-at :as at]
            [rss-feed-reader.core.job.logic :as job]
            [rss-feed-reader.rss.feeder :as rss-feeder]
            [rss-feed-reader.utils.cid :as cid]
            [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]))

(defn- run [job-model fn]
  (cid/set-new)
  (log/info "job started")
  (try
    (let [job (-> (or (job/get-by-name job-model) (job/create job-model))
                  (job/lock))
          from (tc/to-long (t/now))
          job-result (fn)
          to (tc/to-long (t/now))
          execution-ms (- to from)]
      (-> (select-keys job [:job.logic/id :job.logic/version])
          (merge {:job.logic/last-execution-ms execution-ms})
          (job/track_last_execution)
          (job/unlock))
      (log/info "job elapsed in" (format "%dms" execution-ms) job-result))

    (catch Exception e
      (log/error "error while gathering feed items" e)
      (-> job-model
          (job/get-by-name)
          (job/unlock)))))

(def rss-feeder-job
  (partial run
           {:job.logic/name        "feed"
            :job.logic/description "Fetch feed items"}
           rss-feeder/feed))

(def my-pool (at/mk-pool))
(case (:environment env)
  "repl"
  (do
    ; nothing to do here
    )
  "dev"
  (do
    (at/every 5000 rss-feeder-job my-pool :initial-delay 5000))
  "test"
  (do
    ; nothing to do here
    )
  "testing"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000))
  "staging"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000))
  "production"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000)))
