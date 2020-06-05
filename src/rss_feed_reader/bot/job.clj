(ns rss-feed-reader.bot.job
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.bot.client :as client]
            [rss-feed-reader.bot.commands :as commands]
            [rss-feed-reader.scheduler.atat :refer [schedule]]
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

(s/def ::last-offset nat-int?)
(s/fdef run
        :args (s/cat :payload (s/? (s/keys :req [::last-offset])))
        :ret (s/keys :req [::last-offset]))

(schedule :scheduler-bot
          {:job.logic/name        "telegram-get-messages"
           :job.logic/description "Handle telegram updates"}
          run)