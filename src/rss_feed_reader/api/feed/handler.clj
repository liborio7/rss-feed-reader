(ns rss-feed-reader.api.feed.handler
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

(defn domain->response [m]
  (let [{:feed.domain/keys [id link]} m]
    {:feed.api/id   id
     :feed.api/link (str link)}))

;; get

(defn get-feed [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "get feed" req-path)
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
  (let [req-body (:body req)
        link (uris/from-string (:link req-body))]
    (log/info "create feed" req-body)
    (try
      (-> {:feed.domain/link link}
          (mgr/create)
          (domain->response)
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
        id (uuids/from-string (:id req-path))]
    (log/info "delete feed" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (mgr/delete {:feed.domain/id id})
        (r/no-content)))))
