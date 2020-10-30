(ns rss-feed-reader.web.handler.accounts
  (:require [clojure.spec.alpha :as s]
            [rss-feed-reader.utils.uuid :as uuids]
            [clojure.tools.logging :as log]
            [rss-feed-reader.web.response :as response]
            [rss-feed-reader.domain.account :as account]
            [rss-feed-reader.domain.account-feed :as account-feed]
            [rss-feed-reader.domain.feed :as feed]
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

(defn get-account [accounts req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "get account" req-path)
    (if (nil? id)
      (response/not-found)
      (let [account (account/get-by-id accounts {:account.domain/id id})]
        (if (nil? account)
          (response/not-found)
          (->> account
               (account->response-body)
               (response/ok)))))))

(defn get-account-feeds [accounts accounts-feeds req]
  (let [req-path (:path-params req)
        req-query (:params req)
        account-id (uuids/from-string (:account-id req-path))
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get account feeds" req-path req-query)
    (if (nil? account-id)
      (response/not-found)
      (let [account (account/get-by-id accounts {:account.domain/id account-id})]
        (if (nil? account)
          (response/not-found)
          (let [starting-after-account-feed (when starting-after-id
                                              (account-feed/get-by-id accounts-feeds {:account.feed.domain/id starting-after-id}))
                starting-after (if starting-after-account-feed
                                 (:account.feed.domain/order-id starting-after-account-feed)
                                 0)
                limit (if limit
                        (max 0 (min 40 limit))
                        20)
                account-feeds (account-feed/get-by-account accounts-feeds
                                                           {:account.feed.domain/account account}
                                                           {:starting-after starting-after
                                                             :limit          (+ 1 limit)})]
            (->> (response/paginate account-feeds account-feed->response-body limit)
                 (response/ok))))))))

(defn get-account-feed [accounts-feeds req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "get account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (response/not-found)
      (let [account-feed (account-feed/get-by-id accounts-feeds {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (response/not-found)
          (->> account-feed
               (account-feed->response-body)
               (response/ok)))))))

;; post

(defn create-account [accounts req]
  (let [req-body (:body req)
        username (:username req-body)
        chat-id (:chat_id req-body)]
    (log/info "create account" req-body)
    (try
      (->> {:account.domain/username username
            :account.domain/chat-id  chat-id}
           (account/create! accounts)
           (account->response-body)
           (response/ok))
      (catch ExceptionInfo e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:account-logic-create :invalid-spec] (response/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

(defn create-account-feed [accounts accounts-feeds feeds req]
  (let [req-body (:body req)
        req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        link (uris/from-string (:link req-body))]
    (log/info "create account feed" req-path req-body)
    (let [account (account/get-by-id accounts {:account.domain/id account-id})]
      (if (nil? account)
        (response/not-found)
        (try
          (let [feed (feed/create! feeds {:feed.domain/link link})]
            (->> {:account.feed.domain/account account
                  :account.feed.domain/feed    feed}
                 (account-feed/create! accounts-feeds)
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

(defn delete-account [accounts req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))]
    (log/info "delete account" req-path)
    (if (nil? account-id)
      (response/no-content)
      (do
        (account/delete! accounts {:account.domain/id account-id})
        (response/no-content)))))

(defn delete-account-feed [accounts-feeds req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "delete account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (response/not-found)
      (let [account-feed (account-feed/get-by-id accounts-feeds {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (response/no-content)
          (do
            (account-feed/delete! accounts-feeds account-feed)
            (response/no-content)))))))

;; routes

(defn routes [accounts accounts-feeds feeds]
  [
   ["/accounts"
    ["" {:name ::accounts
         :post (partial create-account accounts)}]
    ["/:account-id" {:name   ::account
                     :get    (partial get-account accounts)
                     :delete (partial delete-account accounts)
                     }]
    ["/:account-id/feeds" {:name ::account-feeds
                           :post (partial create-account-feed accounts accounts-feeds feeds)
                           :get  (partial get-account-feeds accounts accounts-feeds)
                           }]
    ["/:account-id/feeds/:account-feed-id" {:name   ::account-feed
                                            :get    (partial get-account-feed accounts-feeds)
                                            :delete (partial delete-account-feed accounts-feeds)
                                            }]]
   ])
