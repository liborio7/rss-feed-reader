(ns rss-feed-reader.jobs
  (:require [rss-feed-reader.env :refer [env]]
            [overtone.at-at :as at]
            [rss-feed-reader.core.job.logic :as job]
            [rss-feed-reader.rss.feeder :as rss-feeder]
            [rss-feed-reader.telegram.updater :as telegram-updater]
            [rss-feed-reader.utils.cid :as cid]
            [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]))

(defn run [model handler]
  (try
    (cid/set-new)
    (let [job (-> (or (job/get-by-name model) (job/create model))
                  (job/lock))
          from (tc/to-long (t/now))
          last-execution-payload (:job.logic/last-execution-payload job)
          execution-payload (if (nil? last-execution-payload)
                              (handler)
                              (handler last-execution-payload))
          to (tc/to-long (t/now))
          execution-ms (- to from)]
      (-> (select-keys job [:job.logic/id :job.logic/version])
          (merge {:job.logic/last-execution-ms      execution-ms
                  :job.logic/last-execution-payload execution-payload})
          (job/track_last_execution)
          (job/unlock))
      (log/info "job elapsed in" (format "%dms" execution-ms) execution-payload))

    (catch Exception e
      (log/error "error while gathering feed items" e)
      (-> model
          (job/get-by-name)
          (job/unlock)))))

(def rss-feeder-job
  (partial run
           {:job.logic/name        "rss-feeder"
            :job.logic/description "Fetch feed items"}
           (partial rss-feeder/feed)))

(def telegram-updater-job
  (partial run {:job.logic/name        "telegram-updater"
                :job.logic/description "Handle telegram updates"}
           (partial telegram-updater/update)))

(def my-pool (at/mk-pool))
(case (:environment env)
  "dev"
  (do
    (at/every 5000 rss-feeder-job my-pool :initial-delay 5000)
    (at/every 1000 telegram-updater-job my-pool :initial-delay 5000))
  "testing"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000)
    (at/every 1000 telegram-updater-job my-pool :initial-delay 5000))
  "staging"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000)
    (at/every 500 telegram-updater-job my-pool :initial-delay 5000))
  "production"
  (do
    (at/every 15000 rss-feeder-job my-pool :initial-delay 5000)
    (at/every 500 telegram-updater-job my-pool :initial-delay 5000)))
