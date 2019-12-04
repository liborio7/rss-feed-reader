(ns rss-feed-reader.api.feeds.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as mgr]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.response :as r]))

;; spec

(s/def :feed.api/id uuid?)
(s/def :feed.api/link string?)

;; conversion

(defn create-feed-req->domain [m]
  (let [{:feed.api/keys [link]} m]
    {:feed.domain/link (uris/from-string link)}))

(defn domain->response [m]
  (let [{:feed.domain/keys [id link]} m]
    {:feed.api/id   id
     :feed.api/link (str link)}))

;; get

(defn get-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "get" req-path)
    (if (nil? id)
      (r/not-found)
      (let [feed (mgr/get-by-id {:feed.domain/id id})]
        (if (nil? feed)
          (r/not-found)
          (-> feed
              (domain->response)
              (r/ok)))))))

;; post

(defn create-feed [req]
  (let [req-body (:body req)]
    (log/info "post" req-body)
    (try
      (->> req-body
           (maps/->q-map "feed.api")
           (create-feed-req->domain)
           (mgr/create)
           (domain->response)
           (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              cause (:cause data)]
          (case cause
            :feed-manager-create (r/bad-request {:code    1
                                                 :message "invalid feed"})
            (throw e)))))))

;; delete

(defn delete-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "delete" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (mgr/delete {:feed.domain/id id})
        (r/no-content)))))
