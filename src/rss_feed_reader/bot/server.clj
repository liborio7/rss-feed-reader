(ns rss-feed-reader.bot.server
  (:require [com.stuartsierra.component :as component]
            [rss-feed-reader.domain.job.scheduler :as job-scheduler]
            [rss-feed-reader.bot.client :as bot]
            [rss-feed-reader.bot.router :as router]
            [clojure.string :as string]
            [clojure.tools.logging :as log]))

(defn handler [bot accounts accounts-feeds feeds]
  (fn [payload]
    (log/trace "pull bot updates")
    (let [offset (or (::last-offset payload) 0)
          updates (bot/get-updates bot (inc offset))
          messages (map :telegram.update/message updates)]
      (doseq [message messages]
        (let [text (-> message
                       (:telegram.message/text)
                       (string/split #" "))
              command (first text)
              args (rest text)
              chat (-> message
                       (:telegram.message/chat))]
          (case command
            "/help" (router/help bot chat)
            "/list" (router/list bot accounts accounts-feeds chat)
            "/add" (router/add bot accounts accounts-feeds feeds chat (first args))
            "/del" (router/del bot accounts accounts-feeds feeds chat (first args))
            (bot/send-message bot chat "Invalid command"))))
      {::messages-count (count messages)
       ::last-offset    (if (empty? updates)
                          offset
                          (:telegram.update/update-id (last updates)))})))

;; component

(defrecord BotServer [config
                      job-scheduler bot accounts accounts-feeds feeds
                      job]
  component/Lifecycle
  (start [this]
    (log/info "start bot server")
    (if job
      this
      (let [model {:job.domain/name        "telegram-get-messages"
                   :job.domain/description "Handle telegram updates"}]
        (assoc this :job (job-scheduler/schedule job-scheduler config model (handler bot accounts accounts-feeds feeds))))))
  (stop [this]
    (log/info "stop bot server")
    (if job
      (do
        (job-scheduler/cancel job-scheduler job)
        (assoc this :job nil))
      this)))