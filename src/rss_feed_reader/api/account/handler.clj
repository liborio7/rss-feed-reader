(ns rss-feed-reader.api.account.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.account :as account-mgr]
            [rss-feed-reader.domain.account_feed :as account-feed-mgr]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.int :as ints]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.uri :as uris]))

;; spec

(s/def :account.api/id uuid?)
(s/def :account.api/username string?)

(s/def :account.feed.api/id uuid?)
(s/def :account.feed.api/link string?)

;; conversion

(defn domain->account-response [m]
  (let [{:account.domain/keys [id username]} m]
    {:account.api/id       id
     :account.api/username username}))

(defn domain->account-feed-response [m]
  (let [{:account.feed.domain/keys [id feed]} m]
    {:account.feed.api/id   id
     :account.feed.api/link (str (:feed.domain/link feed))}))

;; get

(defn get-account [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "get account" req-path)
    (if (nil? id)
      (r/not-found)
      (let [account (account-mgr/get-by-id {:account.domain/id id})]
        (if (nil? account)
          (r/not-found)
          (-> account
              (domain->account-response)
              (r/ok)))))))

(defn get-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "get account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (r/not-found)
      (let [account-feed (account-feed-mgr/get-by-id {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (r/not-found)
          (-> account-feed
              (domain->account-feed-response)
              (r/ok)))))))

;; post

(defn create-account [req]
  (let [req-body (:body req)
        username (:username req-body)]
    (log/info "create account" req-body)
    (try
      (-> {:account.domain/username username}
          (account-mgr/create)
          (domain->account-response)
          (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:account-domain-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

(defn create-account-feed [req]
  (let [req-body (:body req)
        req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        link (uris/from-string (:link req-body))]
    (log/info "create account feed" req-path req-body)
    (let [account (account-mgr/get-by-id {:account.domain/id account-id})]
      (if (nil? account)
        (r/not-found)
        (try
          (let [feed (feed-mgr/create {:feed.domain/link link})]
            (-> {:account.feed.domain/account account
                 :account.feed.domain/feed    feed}
                (account-feed-mgr/create)
                (domain->account-feed-response)
                (r/ok)))
          (catch Exception e
            (let [data (ex-data e)
                  {:keys [cause reason]} data]
              (case [cause reason]
                [:feed-domain-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
                [:account-feed-domain-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
                (throw e)))))))))


(defn get-account-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        account-id (uuids/from-string (:account-id req-path))
        starting-after (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get accounts feeds" req-path req-query)
    (if (nil? account-id)
      (r/not-found)
      (do
        ;; TODO
        []
        ))))

;; delete

(defn delete-account [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))]
    (log/info "delete account" req-path)
    (if (nil? account-id)
      (r/no-content)
      (do
        (account-mgr/delete {:account.domain/id account-id})
        (r/no-content)))))

(defn delete-account-feed [req]
  (let [req-path (:path-params req)
        account-id (uuids/from-string (:account-id req-path))
        account-feed-id (uuids/from-string (:account-feed-id req-path))]
    (log/info "delete account feed" req-path)
    (if (some nil? [account-id account-feed-id])
      (r/not-found)
      (let [account-feed (account-feed-mgr/get-by-id {:account.feed.domain/id account-feed-id})]
        (if (not= account-id (:account.domain/id (:account.feed.domain/account account-feed)))
          (r/no-content)
          (do
            (account-feed-mgr/delete account-feed)
            (r/no-content)))))))