(ns rss-feed-reader.core.feed.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.feed.item.logic :as feed-item-logic]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.date :as dates]
            [rss-feed-reader.utils.int :as ints]))

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

(defn feed-domain-model->api-model [model]
  (let [{:feed.logic/keys [id link]} model]
    {:feed.handler/id   id
     :feed.handler/link (str link)}))

(defn feed-item-domain-model->api-model [model]
  (let [{:feed.item.logic/keys [id title link pub-time description]} model]
    {:feed.item.handler/id          id
     :feed.item.handler/title       title
     :feed.item.handler/link        (str link)
     :feed.item.handler/pub-time    (dates/unparse-date pub-time)
     :feed.item.handler/description description}))

;; get

(defn get-feeds [req]
  (let [req-path (:path-params req)
        req-query (:params req)
        starting-after-id (uuids/from-string (:starting-after req-query))
        limit (ints/parse-int (:limit req-query))]
    (log/info "get feeds" req-path req-query)
    (let [starting-after-feed (if-not (nil? starting-after-id)
                                (feed-logic/get-by-id {:feed.logic/id starting-after-id}))
          starting-after (if-not (nil? starting-after-feed)
                           (:feed.logic/order-id starting-after-feed)
                           0)
          limit (if-not (nil? limit)
                  (max 0 (min 40 limit))
                  20)
          feeds (feed-logic/get-all :starting-after starting-after
                                    :limit (+ 1 limit))]
      (-> (r/paginate feeds feed-domain-model->api-model limit)
          (r/ok)))))

(defn get-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:feed-id req-path))]
    (log/info "get feed" req-path)
    (if (nil? id)
      (r/not-found)
      (let [feed (feed-logic/get-by-id {:feed.logic/id id})]
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
      (let [feed (feed-logic/get-by-id {:feed.logic/id feed-id})]
        (if (nil? feed)
          (r/not-found)
          (let [starting-after-feed-item (if-not (nil? starting-after-id)
                                           (feed-item-logic/get-by-id {:feed.item.logic/id starting-after-id}))
                starting-after (if-not (nil? starting-after-feed-item)
                                 (:feed.item.logic/order-id starting-after-feed-item)
                                 0)
                limit (if-not (nil? limit)
                        (max 0 (min 40 limit))
                        20)
                feed-items (feed-item-logic/get-by-feed {:feed.item.logic/feed feed}
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
      (-> {:feed.logic/link link}
          (feed-logic/create)
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
        (feed-logic/delete {:feed.logic/id id})
        (r/no-content)))))
