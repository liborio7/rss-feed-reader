(ns rss-feed-reader.bot.job
  (:require [mount.core :refer [defstate]]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.bot.client :as client]
            [rss-feed-reader.bot.commands :as commands]
            [rss-feed-reader.scheduler.atat :refer [start stop]]
            [clojure.string :as string]))

(defn run
  ([] (run {::last-offset 0}))
  ([payload]
   (log/trace "run bot" payload)
   (let [offset (::last-offset payload)
         updates (client/get-updates (inc offset))
         messages (map :telegram.update/message updates)]
     (doseq [message messages]
       (let [text (-> message
                      (:telegram.message/text)
                      (string/split #" "))
             command (first text)
             args (rest text)
             chat (-> message
                      (:telegram.message/chat))]
         (commands/execute command chat args)))
     {::messages-count (count messages)
      ::last-offset    (if (empty? updates)
                         offset
                         (:telegram.update/update-id (last updates)))})))

(defn start-job []
  (let [job-model {:job.domain/name        "telegram-get-messages"
                   :job.domain/description "Handle telegram updates"}]
    (log/info "start bot job")
    (start :scheduler-bot job-model run)))

(defn stop-job [job]
  (log/info "stop bot job")
  (stop job))

(defstate job
  :start (start-job)
  :stop (stop-job job))