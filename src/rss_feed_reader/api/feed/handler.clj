(ns rss-feed-reader.api.feed.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.feed_item :as feed-item-mgr]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.date :as dates]
            [rss-feed-reader.utils.int :as ints]))

;; model

(s/def :feed.api/id uuid?)
(s/def :feed.api/link string?)

(s/def ::feed-model (s/keys :req [:feed.api/id
                                  :feed.api/link]))

(s/def :feed.item.api/id uuid?)
(s/def :feed.item.api/title string?)
(s/def :feed.item.api/link string?)
(s/def :feed.item.api/pub-time string?)
(s/def :feed.item.api/description string?)

(s/def ::feed-item-model (s/keys :req [:feed.item.api/id
                                       :feed.item.api/title
                                       :feed.item.api/link]
                                 :opt [:feed.item.api/pub-time
                                       :feed.item.api/description]))

;; conversion

(defn feed-domain-model->api-model [model]
  (let [{:feed.domain/keys [id link]} model]
    {:feed.api/id   id
     :feed.api/link (str link)}))

(defn feed-item-domain-model->api-model [model]
  (let [{:feed.item.domain/keys [id title link pub-time description]} model]
    {:feed.item.api/id          id
     :feed.item.api/title       title
     :feed.item.api/link        (str link)
     :feed.item.api/pub-time    (dates/unparse-date pub-time)
     :feed.item.api/description description}))

;; get

(defn get-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get feeds" req-path req-query)
    (let [starting-after-feed (if-not (nil? starting-after-id)
                                (feed-mgr/get-by-id {:feed.domain/id starting-after-id}))
          starting-after (if-not (nil? starting-after-feed)
                           (:feed.domain/order-id starting-after-feed)
                           0)
          limit (if-not (nil? limit)
                  (max 0 (min 40 limit))
                  20)
          feeds (feed-mgr/get-all :starting-after starting-after
                                  :limit (+ 1 limit))]
      (-> (r/paginate feeds feed-domain-model->api-model limit)
          (r/ok)))))

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
              (feed-domain-model->api-model)
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
                        (max 0 (min 40 limit))
                        20)
                feed-items (feed-item-mgr/get-by-feed {:feed.item.domain/feed feed}
                                                      :starting-after starting-after
                                                      :limit (+ 1 limit))]
            (-> (r/paginate feed-items feed-item-domain-model->api-model limit)
                (r/ok))))))))

;; post

(defn create-feed [req]
  (let [req-body (:body req)
        link (uris/from-string (:link req-body))]
    (log/info "create feed" req-body)
    (try
      (-> {:feed.domain/link link}
          (feed-mgr/create)
          (feed-domain-model->api-model)
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
