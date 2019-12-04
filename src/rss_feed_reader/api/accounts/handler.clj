(ns rss-feed-reader.api.accounts.handler
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.account :as mgr]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.utils.uuid :as uuids]
            [rss-feed-reader.utils.response :as r]))

;; spec

(s/def :account.api/id uuid?)
(s/def :account.api/username string?)

;; conversion

(defn create-account-req->domain [m]
  (let [{:account.api/keys [username]} m]
    {:account.domain/username username}))

(defn domain->response [m]
  (let [{:account.domain/keys [id username]} m]
    {:account.api/id       id
     :account.api/username username}))

;; get

(defn get-account [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "get" req-path)
    (if (nil? id)
      (r/not-found)
      (let [account (mgr/get-by-id {:account.domain/id id})]
        (if (nil? account)
          (r/not-found)
          (-> account
              (domain->response)
              (r/ok)))))))

(defn get-account-feed [req]
  ;; TODO
  )

(defn get-account-feeds [req]
  ;; TODO
  )

;; post

(defn create-account [req]
  (let [req-body (:body req)]
    (log/info "post" req-body)
    (try
      (->> req-body
           (maps/->q-map "account.api")
           (create-account-req->domain)
           (mgr/create)
           (domain->response)
           (r/ok))
      (catch Exception e
        (let [data (ex-data e)
              cause (:cause data)]
          (case cause
            :account-manager-create (r/bad-request {:code    2
                                                    :message "invalid account"})))))))

(defn create-account-feed [req]
  ;; TODO
  )

;; delete

(defn delete-account [req]
  (let [req-path (:path-params req)
        id (uuids/from-string (:account-id req-path))]
    (log/info "delete" req-path)
    (if (nil? id)
      (r/no-content)
      (do
        (mgr/delete {:account.domain/id id})
        (r/no-content)))))

(defn delete-account-feed [req]
  ;; TODO
  )