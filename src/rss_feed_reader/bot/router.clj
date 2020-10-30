(ns rss-feed-reader.bot.router
  (:refer-clojure :exclude [list])
  (:require [rss-feed-reader.bot.client :as bot]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.account :as account]
            [rss-feed-reader.domain.account-feed :as account-feed]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.rss.parser :as rss]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.string :as string]))

(defn help [bot chat]
  (let [chat-id (:telegram.message.chat/id chat)]
    (bot/send-message bot chat-id "Available commands:
          /list        Show RSS feeds subscriptions
          /add [rss]   Subscribe to RSS feed URL
          /del [rss]   Unsubscribe to RSS feed URL")))

(defn list [bot accounts accounts-feeds chat]
  (log/trace "list" chat "subscriptions")
  (let [chat-id (:telegram.message.chat/id chat)
        feeds-links (->> chat-id
                         (assoc {} :account.domain/chat-id)
                         (account/get-by-chat-id accounts)
                         (assoc {} :account.feed.domain/account)
                         (account-feed/get-by-account accounts-feeds)
                         (map :account.feed.domain/feed)
                         (map (comp str :feed.domain/link))
                         (reduce conj []))]
    (bot/send-message bot chat-id (str "RSS feed links:\n"
                                       (string/join "\n" feeds-links)))))

(defn add [bot accounts accounts-feeds feeds chat rss]
  (log/trace "add" rss "for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (or (nil? link) (empty? (rss/parse rss)))
      (bot/send-message bot chat-id "Usage: /add [rss]
            - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (account/create! accounts {:account.domain/username username
                                               :account.domain/chat-id  chat-id})
            feed (feed/create! feeds {:feed.domain/link link})]
        (account-feed/create! accounts-feeds {:account.feed.domain/account account
                                              :account.feed.domain/feed    feed})
        (bot/send-message bot chat-id (format "RSS %s added" rss))))))

(defn del [bot accounts accounts-feeds feeds chat rss]
  (log/trace "delete" rss " for chat" chat)
  (let [chat-id (:telegram.message.chat/id chat)
        link (uris/from-string rss)]
    (if (nil? link)
      (bot/send-message bot chat-id "Usage: /del [rss]
            - [rss] should be a valid RSS feed URL")
      (let [username (:telegram.message.chat/username chat)
            account (account/create! accounts {:account.domain/username username
                                               :account.domain/chat-id  chat-id})
            feed (feed/get-by-link feeds {:feed.domain/link link})
            account-feed (account-feed/get-by-account-and-feed accounts-feeds {:account.feed.domain/account account
                                                                               :account.feed.domain/feed    feed})]
        (when account-feed
          (account-feed/delete! accounts-feeds account-feed))
        (bot/send-message bot chat-id (format "RSS %s deleted" rss))))))