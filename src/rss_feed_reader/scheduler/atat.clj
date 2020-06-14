(ns rss-feed-reader.scheduler.atat
  (:require [rss-feed-reader.env :refer [env]]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.domain.job.logic :as job]
            [overtone.at-at :as at]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clojure.tools.logging :as log]))

(defonce pool (delay (at/mk-pool)))

(defn wrap-handler [job-model handler]
  (fn []
    (try
      (cid/set-new)
      (let [job (-> (or (job/get-by-name job-model) (job/create job-model))
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
        (-> job-model
            (job/get-by-name)
            (job/unlock))))))

(defn schedule [job model handler]
  (let [handler (wrap-handler model handler)
        config (job env)]
    (log/info "job" job "for env" (:environment env) "has the following configurations:" config)
    (when-not (empty? config)
      (let [{:keys [ms-period initial-delay]} config]
        (at/every ms-period handler @pool :initial-delay initial-delay)))))