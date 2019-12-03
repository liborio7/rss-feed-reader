(ns rss-feed-reader.feed.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.feed.manager :as mgr]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.response :as r]))

;; spec

(s/def :feed.api/id uuid?)
(s/def :feed.api/title string?)
(s/def :feed.api/link string?)
(s/def :feed.api/description string?)

;; conversion

(defn post-api->logic [req]
  (let [{:feed.api/keys [title link description]} req]
    {:feed.logic/title       title
     :feed.logic/link        (uris/from-string link)
     :feed.logic/description description}))

(defn logic->response [logic]
  (let [{:feed.logic/keys [id title link description]} logic]
    {:feed.api/id          id
     :feed.api/title       title
     :feed.api/link        (str link)
     :feed.api/description description}))

;; post

(defn post [req]
  (let [req-body (:body req)]
    (log/info "post" req-body)
    (try
      (->> req-body
           (maps/->q-map "feed.api")
           (post-api->logic)
           (mgr/create)
           (logic->response)
           (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              cause (:cause data)]
          (case cause
            :feed-manager-create (r/bad-request {:code    1
                                                 :message "invalid feed"})))))))


;; get

(defn get-by-id [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "get" req-path)
    (if (nil? id)
      (r/not-found)
      (let [feed (mgr/get-by-id {:feed.logic/id id})]
        (if (nil? feed)
          (r/not-found)
          (r/ok feed))))))

;; delete

(defn delete [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "delete" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (mgr/delete {:feed.logic/id id})
        (r/no-content)))))
