(ns rss-feed-reader.telegram.updater
  (:refer-clojure :exclude [update list])
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [rss-feed-reader.telegram.client :as telegram]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.account.logic :as account-logic]
            [rss-feed-reader.core.account.feed.logic :as account-feed-logic]
            [rss-feed-reader.rss.parser :as rss]
            [rss-feed-reader.utils.uri :as uris]))

(s/def ::last-offset nat-int?)

(defn help [chat]
  (let [chat-id (:telegram.message.chat/id chat)]
    (telegram/send-message chat-id "Available commands:
  /list         Show RSS feeds subscriptions
  /add [rss]   Subscribe to RSS feed URL
  /del [rss]   Unsubscribe to RSS feed URL")))

(defn default [chat cmd]
  (let [chat-id (:telegram.message.chat/id chat)]
    (telegram/send-message chat-id (format "Sorry, I can't understand command \"%s\" :(" cmd))))

(defn list [chat]
  (log/trace "list" chat "subscriptions")
  (let [chat-id (:telegram.message.chat/id chat)
        feeds-links (->> chat-id
                         (assoc {} :account.logic/chat-id)
                         (account-logic/get-by-chat-id)
                         (assoc {} :account.feed.logic/account)
                         (account-feed-logic/get-by-account)
                         (map :account.feed.logic/feed)
                         (map (comp str :feed.logic/link))
                         (reduce conj []))]
    (telegram/send-message chat-id (str "RSS feed links:\n"
                                        (string/join "\n" feeds-links)))))

(defn add [chat rss]
  (log/trace "add" rss "for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (or (nil? link) (empty? (rss/parse rss)))
      (telegram/send-message chat-id "Usage: /add [rss]
      - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (account-logic/create {:account.logic/username username
                                           :account.logic/chat-id  chat-id})
            feed (feed-logic/create {:feed.logic/link link})]
        (account-feed-logic/create {:account.feed.logic/account account
                                    :account.feed.logic/feed    feed})
        (telegram/send-message chat-id (format "RSS \"%s\" added" rss))))))

(defn del [chat rss]
  (log/trace "delete" rss " for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)]
    (if (empty? rss)
      (telegram/send-message chat-id "Usage: /del [rss]")
      (telegram/send-message chat-id (format "RSS \"%s\" deleted" rss)))))

(defn update
  ([] (update {::last-offset 0}))
  ([payload]
   (log/trace "start updating" payload)
   (let [offset (::last-offset payload)
         updates (telegram/get-updates (inc offset))
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
           "/help" (help chat)
           "/list" (list chat)
           "/add" (add chat (first args))
           "/del" (del chat (first args))
           (default chat command))))
     {::messages-count (count messages)
      ::last-offset    (if (empty? updates)
                         offset
                         (:telegram.update/update-id (last updates)))})))

(s/fdef update
        :args (s/cat :payload (s/? (s/keys :req [::last-offset])))
        :ret (s/keys :req [::last-offset]))