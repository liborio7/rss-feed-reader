(ns rss-feed-reader.api.feed.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.feed_item :as feed-item-mgr]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.date :as dates]
            [rss-feed-reader.utils.int :as ints]
            [rss-feed-reader.domain.account_feed :as account-feed-mgr]))

;; spec

(s/def :feed.api/id uuid?)
(s/def :feed.api/link string?)

;; conversion

(defn domain->feed-response [m]
  (let [{:feed.domain/keys [id link]} m]
    {:feed.api/id   id
     :feed.api/link (str link)}))

(defn domain->feed-item-response [m]
  (let [{:feed.item.domain/keys [id title link pub-time description]} m]
    {:feed.api/id          id
     :feed.api/title       title
     :feed.api/link        link
     :feed.api/pub-time    (dates/unparse-date pub-time)
     :feed.api/description description}))

;; get

(defn get-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:feed-id req-path))]
    (log/info "get feed" req-path)
    (if (nil? id)
      (r/not-found)
      (let [feed (feed-mgr/get-by-id {:feed.domain/id id})]
        (if (nil? feed)
          (r/not-found)
          (-> feed
              (domain->feed-response)
              (r/ok)))))))

(defn get-feed-items [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        feed-id (uuids/from-string (:feed-id req-path))
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get feed items" req-path req-query)
    (if (nil? feed-id)
      (r/not-found)
      (let [feed (feed-mgr/get-by-id {:feed.domain/id feed-id})]
        (if (nil? feed)
          (r/not-found)
          (let [starting-after-feed-item (if-not (nil? starting-after-id)
                                           (feed-item-mgr/get-by-id {:feed.item.domain/id starting-after-id}))
                starting-after (if-not (nil? starting-after-feed-item)
                                 (:feed.item.domain/order-id starting-after-feed-item)
                                 0)
                limit (if-not (nil? limit)
                        (max 0 (min 20 limit))
                        20)
                feed-items (feed-item-mgr/get-by-feed {:feed.item.domain/feed           feed
                                                       :feed.item.domain/starting-after starting-after
                                                       :feed.item.domain/limit          (+ 1 limit)})]
            (r/ok {:feed.item.api/data     (->> feed-items
                                                (map domain->feed-item-response)
                                                (take limit))
                   :feed.item.api/has-more (> (count feed-items) limit)})))))))

;; post

(defn create-feed [req]
  (let [req-body (:body req)
        link (uris/from-string (:link req-body))]
    (log/info "create feed" req-body)
    (try
      (-> {:feed.domain/link link}
          (feed-mgr/create)
          (domain->feed-response)
          (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:feed-domain-create :invalid-spec] (r/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

;; delete

(defn delete-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:feed-id req-path))]
    (log/info "delete feed" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (feed-mgr/delete {:feed.domain/id id})
        (r/no-content)))))
