(ns rss-feed-reader.domain.job.scheduler
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [rss-feed-reader.domain.job :as job]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as time]
            [rss-feed-reader.utils.cid :as cid]))

(defn- wrap-handler [jobs model handler]
  (fn []
    (try
      (cid/set-new)
      (let [job (as-> (or (job/get-by-name jobs model) (job/create! jobs model)) job
                      (job/toggle-lock! jobs job true))
            from (time/instant->long (time/instant-now))
            last-execution-payload (:job.domain/last-execution-payload job)
            execution-payload (handler last-execution-payload)
            to (time/instant->long (time/instant-now))
            execution-ms (- to from)]
        (as-> job job
              (job/track-last-execution! jobs job execution-payload execution-ms)
              (job/toggle-lock! jobs job false))
        (log/info "job elapsed in" (format "%dms" execution-ms) execution-payload))

      (catch Exception e
        (log/error "error while executing job" model e)
        (as-> model job
              (job/get-by-name jobs job)
              (job/toggle-lock! jobs job false))))))

;; component

(defprotocol Scheduler
  (schedule [this config model handler])
  (cancel [this job-info]))

(defrecord JobScheduler [jobs
                         pool]
  component/Lifecycle
  (start [this]
    (log/info "start scheduler")
    (if pool
      this
      (let [pool (at-at/mk-pool)]
        (assoc this :pool pool))))
  (stop [this]
    (log/info "stop scheduler")
    (if pool
      (do
        (at-at/stop-and-reset-pool! pool)
        (assoc this :pool nil))
      this))

  Scheduler
  (schedule [_ {:keys [period initial-delay]} model handler]
    (let [handler (wrap-handler jobs model handler)]
      (at-at/every period handler pool :initial-delay initial-delay)))

  (cancel [_ job-info]
    (at-at/stop (:id job-info) pool)))