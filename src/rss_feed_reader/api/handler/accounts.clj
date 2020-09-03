(ns rss-feed-reader.api.handler.accounts
  (:require [clojure.spec.alpha :as s]
            [rss-feed-reader.utils.uuid :as uuids]
            [clojure.tools.logging :as log]
            [rss-feed-reader.api.response :as response]
            [rss-feed-reader.domain.account :as accounts]
            [rss-feed-reader.domain.account-feed :as account-feeds]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.int :as ints])
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

(defn- account->response-body [model]
  (let [{:account.domain/keys [id username chat-id]} model]
    {:account.handler/id       id
     :account.handler/username username
     :account.handler/chat-id  chat-id}))

(defn- account-feed->response-body [model]
  (let [{:account.feed.domain/keys [id feed]} model]
    {:account.feed.handler/id   id
     :account.feed.handler/link (str (:feed.domain/link feed))}))

;; get

(defn get-account [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "get account" req-path)
    (if (nil? id)
      (response/not-found)
      (let [account (accounts/get-by-id {:account.domain/id id})]
        (if (nil? account)
          (response/not-found)
          (-> account
              (account->response-body)
              (response/ok)))))))

(defn get-account-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        account-id (uuids/from-string (:account-id req-path))
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get account feeds" req-path req-query)
    (if (nil? account-id)
      (response/not-found)
      (let [account (accounts/get-by-id {:account.domain/id account-id})]
        (if (nil? account)
          (response/not-found)
          (let [starting-after-account-feed (when starting-after-id
                                              (account-feeds/get-by-id {:account.feed.domain/id starting-after-id}))
                starting-after (if starting-after-account-feed
                                 (:account.feed.domain/order-id starting-after-account-feed)
                                 0)
                limit (if limit
                        (max 0 (min 40 limit))
                        20)
                account-feeds (account-feeds/get-by-account {:account.feed.domain/account account}
                                                            :starting-after starting-after
                                                            :limit (+ 1 limit))]
            (-> (response/paginate account-feeds account-feed->response-body limit)
                (response/ok))))))))

(defn get-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "get account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (response/not-found)
      (let [account-feed (account-feeds/get-by-id {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (response/not-found)
          (-> account-feed
              (account-feed->response-body)
              (response/ok)))))))

;; post

(defn create-account [req]
  (let [req-body (:body req)
        username (:username req-body)
        chat-id (:chat_id req-body)]
    (log/info "create account" req-body)
    (try
      (-> {:account.domain/username username
           :account.domain/chat-id  chat-id}
          (accounts/create!)
          (account->response-body)
          (response/ok))
      (catch ExceptionInfo e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:account-logic-create :invalid-spec] (response/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

(defn create-account-feed [req]
  (let [req-body (:body req)
        req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        link (uris/from-string (:link req-body))]
    (log/info "create account feed" req-path req-body)
    (let [account (accounts/get-by-id {:account.domain/id account-id})]
      (if (nil? account)
        (response/not-found)
        (try
          (let [feed (feeds/create! {:feed.domain/link link})]
            (-> {:account.feed.domain/account account
                 :account.feed.domain/feed    feed}
                (account-feeds/create!)
                (account-feed->response-body)
                (response/ok)))
          (catch ExceptionInfo e
            (let [data (ex-data e)
                  {:keys [cause reason]} data]
              (case [cause reason]
                [:feed-logic-create :invalid-spec] (response/bad-request {:code 1 :message "invalid request"})
                [:account-feed-logic-create :invalid-spec] (response/bad-request {:code 1 :message "invalid request"})
                (throw e)))))))))

;; delete

(defn delete-account [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))]
    (log/info "delete account" req-path)
    (if (nil? account-id)
      (response/no-content)
      (do
        (accounts/delete! {:account.domain/id account-id})
        (response/no-content)))))

(defn delete-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "delete account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (response/not-found)
      (let [account-feed (account-feeds/get-by-id {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (response/no-content)
          (do
            (account-feeds/delete! account-feed)
            (response/no-content)))))))

;; routes

(def routes
  [
   ["/accounts"
    ["" {:name ::accounts
         :post create-account}]
    ["/:account-id" {:name   ::account
                     :get    get-account
                     :delete delete-account
                     }]
    ["/:account-id/feeds" {:name ::account-feeds
                           :post create-account-feed
                           :get  get-account-feeds
                           }]
    ["/:account-id/feeds/:account-feed-id" {:name   ::account-feed
                                            :get    get-account-feed
                                            :delete delete-account-feed
                                            }]]
   ])
