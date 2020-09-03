(ns rss-feed-reader.api.handler.feeds
  (:require [clojure.spec.alpha :as s]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.int :as ints]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.api.response :as response]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.time :as t]
            [rss-feed-reader.domain.feed-item :as feed-items])
  (:import (clojure.lang ExceptionInfo)))


;; model

(s/def :feed.handler/id uuid?)
(s/def :feed.handler/link string?)

(s/def ::model (s/keys :req [:feed.handler/id
                             :feed.handler/link]))

(s/def :feed.item.handler/id uuid?)
(s/def :feed.item.handler/title string?)
(s/def :feed.item.handler/link string?)
(s/def :feed.item.handler/pub-time string?)
(s/def :feed.item.handler/description string?)

(s/def ::item.model (s/keys :req [:feed.item.handler/id
                                  :feed.item.handler/title
                                  :feed.item.handler/link]
                            :opt [:feed.item.handler/pub-time
                                  :feed.item.handler/description]))

;; conversion
(defn feed->response-body [model]
  (let [{:feed.domain/keys [id link]} model]
    {:feed.handler/id   id
     :feed.handler/link (str link)}))

(defn feed-item->response-body [model]
  (let [{:feed.item.domain/keys [id title link pub-time description]} model]
    {:feed.item.handler/id          id
     :feed.item.handler/title       title
     :feed.item.handler/link        (str link)
     :feed.item.handler/pub-time    (t/parse pub-time)
     :feed.item.handler/description description}))

;; get

(defn get-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get feeds" req-path req-query)
    (let [starting-after-feed (when starting-after-id
                                (feeds/get-by-id {:feed.domain/id starting-after-id}))
          starting-after (if starting-after-feed
                           (:feed.domain/order-id starting-after-feed)
                           0)
          limit (if limit
                  (max 0 (min 40 limit))
                  20)
          feeds (feeds/get-all :starting-after starting-after
                               :limit (+ 1 limit))]
      (-> (response/paginate feeds feed->response-body limit)
          (response/ok)))))

(defn get-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:feed-id req-path))]
    (log/info "get feed" req-path)
    (if (nil? id)
      (response/not-found)
      (let [feed (feeds/get-by-id {:feed.domain/id id})]
        (if (nil? feed)
          (response/not-found)
          (-> feed
              (feed->response-body)
              (response/ok)))))))

(defn get-feed-items [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        feed-id (uuids/from-string (:feed-id req-path))
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get feed items" req-path req-query)
    (if (nil? feed-id)
      (response/not-found)
      (let [feed (feeds/get-by-id {:feed.domain/id feed-id})]
        (if (nil? feed)
          (response/not-found)
          (let [starting-after-feed-item (when starting-after-id
                                           (feed-items/get-by-id {:feed.item.domain/id starting-after-id}))
                starting-after (if starting-after-feed-item
                                 (:feed.item.domain/order-id starting-after-feed-item)
                                 0)
                limit (if limit
                        (max 0 (min 40 limit))
                        20)
                feed-items (feed-items/get-by-feed {:feed.item.domain/feed feed}
                                                   :starting-after starting-after
                                                   :limit (+ 1 limit))]
            (-> (response/paginate feed-items feed-item->response-body limit)
                (response/ok))))))))

;; post

(defn create-feed [req]
  (let [req-body (:body req)
        link (uris/from-string (:link req-body))]
    (log/info "create feed" req-body)
    (try
      (-> {:feed.domain/link link}
          (feeds/create!)
          (feed->response-body)
          (response/ok))
      (catch ExceptionInfo e
        (let [data (ex-data e)
              {:keys [cause reason]} data]
          (case [cause reason]
            [:feed-logic-create :invalid-spec] (response/bad-request {:code 1 :message "invalid request"})
            (throw e)))))))

;; delete

(defn delete-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:feed-id req-path))]
    (log/info "delete feed" req-path)
    (if (nil? id)
      (response/no-content)
      (do
        (feeds/delete! {:feed.domain/id id})
        (response/no-content)))))

;; routes

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :get  get-feeds
         :post create-feed}]
    ["/:feed-id" {:name   ::feeds-id
                  :get    get-feed
                  :delete delete-feed
                  }]
    ["/:feed-id/items" {:name ::feed-items
                        :get  get-feed-items
                        }]
    ]])