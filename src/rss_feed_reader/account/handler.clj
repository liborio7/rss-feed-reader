(ns rss-feed-reader.account.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.account.manager :as mgr]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.response :as r]))

;; spec

(s/def :account.api/id uuid?)
(s/def :account.api/username string?)

;; conversion

(defn post-api->manager [m]
  (let [{:account.api/keys [username]} m]
    {:account.manager/username username}))

(defn manager->response [m]
  (let [{:account.manager/keys [id username]} m]
    {:account.api/id       id
     :account.api/username username}))

;; get

(defn get-by-id [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "get" req-path)
    (if (nil? id)
      (r/not-found)
      (let [account (mgr/get-by-id {:account.manager/id id})]
        (if (nil? account)
          (r/not-found)
          (-> account
              (manager->response)
              (r/ok)))))))

;; post

(defn post [req]
  (let [req-body (:body req)]
    (log/info "post" req-body)
    (try
      (->> req-body
           (maps/->q-map "account.api")
           (post-api->manager)
           (mgr/create)
           (manager->response)
           (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              cause (:cause data)]
          (case cause
            :account-manager-create (r/bad-request {:code    2
                                                    :message "invalid account"})))))))

;; delete

(defn delete [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:id req-path))]
    (log/info "delete" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (mgr/delete {:account.manager/id id})
        (r/no-content)))))
