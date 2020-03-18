(ns rss-feed-reader.telegram.updater
  (:refer-clojure :exclude [update])
  (:require [clojure.tools.logging :as log]
            [clojure.string :as str]
            [rss-feed-reader.telegram.client :as telegram]
            [clojure.spec.alpha :as s]))

(s/def ::last-offset nat-int?)

(defn help [chat-id]
  )

(defn default [chat-id cmd]
  (telegram/send-message chat-id (format "Sorry, I can't understand command \"%s\" :(" cmd)))


(defn add [chat-id link]
  (log/trace "add" link "for chat id" chat-id)
  (if (empty? link)
    (telegram/send-message chat-id "usage: /add [url]")
    (telegram/send-message chat-id (format "RSS \"%s\" added" link))))

(defn list [chat-id]
  )

(defn del [chat-id id-or-link]
  )

(defn update
  ([] (update {::last-offset 0}))
  ([payload]
   (log/trace "start updating" payload)
   (let [offset (-> payload
                    (::last-offset)
                    (inc))
         updates (telegram/get-updates offset)
         messages (map :telegram.update/message updates)]
     (doseq [message messages]
       (let [text (-> message
                      (:telegram.message/text)
                      (str/split #" "))
             command (first text)
             args (rest text)
             chat-id (-> message
                         (:telegram.message/chat)
                         (:telegram.message.chat/id))]
         (case command
           "/add" (add chat-id (first args))
           (default chat-id command))))
     {::messages-count (count messages)
      ::last-offset    (if (empty? updates)
                         offset
                         (:telegram.update/update-id (last updates)))})))

(s/fdef update
        :args (s/cat :payload (s/? (s/keys :req [::last-offset])))
        :ret (s/keys :req [::last-offset]))