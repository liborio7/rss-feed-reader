(ns rss-feed-reader.bot.commands
  (:require [rss-feed-reader.bot.client :as bot]
            [rss-feed-reader.domain.feed.logic :as feed-logic]
            [rss-feed-reader.domain.account.logic :as account-logic]
            [rss-feed-reader.domain.account.feed.logic :as account-feed-logic]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.rss.parser :as rss]))

(defmulti execute (fn [cmd & _] cmd))

(defmethod execute "/help" [_ chat]
  (let [chat-id (:telegram.message.chat/id chat)]
    (bot/send-message chat-id "Available commands:
  /list        Show RSS feeds subscriptions
  /add [rss]   Subscribe to RSS feed URL
  /del [rss]   Unsubscribe to RSS feed URL")))

(defmethod execute "/list" [_ chat]
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
    (bot/send-message chat-id (str "RSS feed links:\n"
                                   (string/join "\n" feeds-links)))))

(defmethod execute "/add" [_ chat [rss]]
  (log/trace "add" rss "for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (or (nil? link) (empty? (rss/parse rss)))
      (bot/send-message chat-id "Usage: /add [rss]
      - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (account-logic/create {:account.logic/username username
                                           :account.logic/chat-id  chat-id})
            feed (feed-logic/create {:feed.logic/link link})]
        (account-feed-logic/create {:account.feed.logic/account account
                                    :account.feed.logic/feed    feed})
        (bot/send-message chat-id (format "RSS %s added" rss))))))

(defmethod execute "/del" [_ chat rss]
  (log/trace "delete" rss " for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (nil? link)
      (bot/send-message chat-id "Usage: /del [rss]
      - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (account-logic/create {:account.logic/username username
                                           :account.logic/chat-id  chat-id})
            feed (feed-logic/get-by-link {:feed.logic/link link})
            account-feed (account-feed-logic/get-by-account-and-feed {:account.feed.logic/account account
                                                                      :account.feed.logic/feed    feed})]
        (when account-feed
          (account-feed-logic/delete account-feed))
        (bot/send-message chat-id (format "RSS %s deleted" rss))))))
