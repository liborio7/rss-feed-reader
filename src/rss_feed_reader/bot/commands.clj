(ns rss-feed-reader.bot.commands
  (:require [rss-feed-reader.bot.client :as bot]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.domain.account :as accounts]
            [rss-feed-reader.domain.account-feed :as account-feeds]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.rss.parser :as rss]))

(defmulti execute (fn [cmd & _] cmd))

(defmethod execute "/help" [_ chat _]
  (let [chat-id (:telegram.message.chat/id chat)]
    (bot/send-message chat-id "Available commands:
  /list        Show RSS feeds subscriptions
  /add [rss]   Subscribe to RSS feed URL
  /del [rss]   Unsubscribe to RSS feed URL")))

(defmethod execute "/list" [_ chat _]
  (log/trace "list" chat "subscriptions")
  (let [chat-id (:telegram.message.chat/id chat)
        feeds-links (->> chat-id
                         (assoc {} :account.domain/chat-id)
                         (accounts/get-by-chat-id)
                         (assoc {} :account.feed.domain/account)
                         (account-feeds/get-by-account)
                         (map :account.feed.domain/feed)
                         (map (comp str :feed.domain/link))
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
            account (accounts/create! {:account.domain/username username
                                       :account.domain/chat-id  chat-id})
            feed (feeds/create! {:feed.domain/link link})]
        (account-feeds/create! {:account.feed.domain/account account
                                :account.feed.domain/feed    feed})
        (bot/send-message chat-id (format "RSS %s added" rss))))))

(defmethod execute "/del" [_ chat [rss]]
  (log/trace "delete" rss " for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (nil? link)
      (bot/send-message chat-id "Usage: /del [rss]
      - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (accounts/create! {:account.domain/username username
                                       :account.domain/chat-id  chat-id})
            feed (feeds/get-by-link {:feed.domain/link link})
            account-feed (account-feeds/get-by-account-and-feed {:account.feed.domain/account account
                                                                 :account.feed.domain/feed    feed})]
        (when account-feed
          (account-feeds/delete! account-feed))
        (bot/send-message chat-id (format "RSS %s deleted" rss))))))
