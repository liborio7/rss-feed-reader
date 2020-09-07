(ns rss-feed-reader.scheduler.executor
  (:require [mount.core :refer [defstate]]
            [rss-feed-reader.env :refer [env]]
            [rss-feed-reader.db.datasource :refer [ds]]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.domain.job :as jobs]
            [overtone.at-at :as at]
            [rss-feed-reader.utils.time :as t]
            [clojure.tools.logging :as log]))

(defn start-pool []
  (log/info "start pool")
  (at/mk-pool))

(defn stop-pool [pool]
  (log/info "stop pool")
  (at/stop-and-reset-pool! pool))

(defstate pool
  :start (start-pool)
  :stop (stop-pool pool))

(defn wrap-handler [job-model job-fn]
  (fn []
    (try
      (cid/set-new)
      (let [job (-> (or (jobs/get-by-name job-model) (jobs/create! job-model))
                    (jobs/toggle-lock! true))
            from (t/instant->long (t/instant-now))
            last-execution-payload (:job.domain/last-execution-payload job)
            execution-payload (if last-execution-payload
                                (job-fn last-execution-payload)
                                (job-fn))
            to (t/instant->long (t/instant-now))
            execution-ms (- to from)]
        (-> job
            (jobs/track-last-execution! execution-payload execution-ms)
            (jobs/toggle-lock! false))
        (log/info "job elapsed in" (format "%dms" execution-ms) execution-payload))

      (catch Exception e
        (log/error "error while gathering feed items" e)
        (-> job-model
            (jobs/get-by-name)
            (jobs/toggle-lock! false))))))

(defn start [keyword model handler]
  (let [handler (wrap-handler model handler)
        config (keyword env)]
    (log/info "job" keyword "for env" (:environment env) "has the following configurations:" config)
    (when-not (empty? config)
      (let [{:keys [ms-period initial-delay]} config]
        (at/every ms-period handler pool :initial-delay initial-delay)))))

(defn stop [job-info]
  (at/stop job-info))