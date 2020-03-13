(ns rss-feed-reader.core.account.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.core.account.logic :as account-logic]
            [rss-feed-reader.core.account.feed.logic :as account-feed-logic]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.int :as ints]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.uri :as uris])
  (:import (clojure.lang ExceptionInfo)))

;; model

(s/def :account.handler/id uuid?)
(s/def :account.handler/username string?)
(s/def :account.handler/chat-id int?)

(s/def ::model (s/cat :req [:account.handler/id
                            :account.handler/username
                            :account.handler/chat-id]))

(s/def :account.feed.handler/id uuid?)
(s/def :account.feed.handler/link string?)

(s/def ::feed.model (s/cat :req [:account.feed.handler/id
                                 :account.feed.handler/link]))

;; conversion

(defn account-logic-model->handler-model [model]
  (let [{:account.logic/keys [id username chat-id]} model]
    {:account.handler/id       id
     :account.handler/username username
     :account.handler/chat-id  chat-id}))

(defn account-feed-logic-model->handler-model [model]
  (let [{:account.feed.logic/keys [id feed]} model]
    {:account.feed.handler/id   id
     :account.feed.handler/link (str (:feed.logic/link feed))}))

;; get

(defn get-account [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "get account" req-path)
    (if (nil? id)
      (r/not-found)
      (let [account (account-logic/get-by-id {:account.logic/id id})]
        (if (nil? account)
          (r/not-found)
          (-> account
              (account-logic-model->handler-model)
              (r/ok)))))))

(defn get-account-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        account-id (uuids/from-string (:account-id req-path))
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get account feeds" req-path req-query)
    (if (nil? account-id)
      (r/not-found)
      (let [account (account-logic/get-by-id {:account.logic/id account-id})]
        (if (nil? account)
          (r/not-found)
          (let [starting-after-account-feed (if-not (nil? starting-after-id)
                                              (account-feed-logic/get-by-id {:account.feed.logic/id starting-after-id}))
                starting-after (if-not (nil? starting-after-account-feed)
                                 (:account.feed.logic/order-id starting-after-account-feed)
                                 0)
                limit (if-not (nil? limit)
                        (max 0 (min 40 limit))
                        20)
                account-feeds (account-feed-logic/get-by-account {:account.feed.logic/account account}
                                                                 :starting-after starting-after
                                                                 :limit (+ 1 limit))]
            (-> (r/paginate account-feeds account-feed-logic-model->handler-model limit)
                (r/ok))))))))

(defn get-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "get account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (r/not-found)
      (let [account-feed (account-feed-logic/get-by-id {:account.feed.logic/id account-feed-id})]
        (if (not= account-id (:account.logic/id (:account.feed.logic/account account-feed)))
          (r/not-found)
          (-> account-feed
              (account-feed-logic-model->handler-model)
              (r/ok)))))))

;; post

(defn create-account [req]
  (let [req-body (:body req)
        username (:username req-body)
        chat-id (:chat_id req-body)]
    (log/info "create account" req-body)
    (try
      (-> {:account.logic/username username
           :account.logic/chat-id  chat-id}
          (account-logic/create)
          (account-logic-model->handler-model)
          (r/ok))
      (catch ExceptionInfo e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:account-logic-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

(defn create-account-feed [req]
  (let [req-body (:body req)
        req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        link (uris/from-string (:link req-body))]
    (log/info "create account feed" req-path req-body)
    (let [account (account-logic/get-by-id {:account.logic/id account-id})]
      (if (nil? account)
        (r/not-found)
        (try
          (let [feed (feed-logic/create {:feed.logic/link link})]
            (-> {:account.feed.logic/account account
                 :account.feed.logic/feed    feed}
                (account-feed-logic/create)
                (account-feed-logic-model->handler-model)
                (r/ok)))
          (catch ExceptionInfo e
            (let [data (ex-data e)
                  {:keys [cause reason]} data]
              (case [cause reason]
                [:feed-logic-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
                [:account-feed-logic-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
                (throw e)))))))))

;; delete

(defn delete-account [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))]
    (log/info "delete account" req-path)
    (if (nil? account-id)
      (r/no-content)
      (do
        (account-logic/delete {:account.logic/id account-id})
        (r/no-content)))))

(defn delete-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "delete account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (r/not-found)
      (let [account-feed (account-feed-logic/get-by-id {:account.feed.logic/id account-feed-id})]
        (if (not= account-id (:account.logic/id (:account.feed.logic/account account-feed)))
          (r/no-content)
          (do
            (account-feed-logic/delete account-feed)
            (r/no-content)))))))