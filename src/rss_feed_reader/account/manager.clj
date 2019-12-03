(ns rss-feed-reader.account.manager
  (:require [rss-feed-reader.account.dao :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :account.manager/id uuid?)
(s/def :account.manager/version int?)
(s/def :account.manager/insert-time inst?)
(s/def :account.manager/update-time inst?)
(s/def :account.manager/username string?)


(s/def ::get-by-id-req (s/keys :req [:account.manager/id]))

(s/def ::delete-req (s/keys :req [:account.manager/id]))

(s/def ::create-req (s/keys :req [:account.manager/username]
                            :opt [:account.manager/id
                                  :account.manager/version
                                  :account.manager/insert-time
                                  :account.manager/update-time]))

(s/def ::resp (s/keys :req [:account.manager/id
                            :account.manager/username]))

;; conversion

(defn create-req->model [m]
  (let [{:account.manager/keys [id version insert-time update-time username]
         :or                   {id          (java.util.UUID/randomUUID)
                                version     0
                                insert-time (tc/to-long (t/now))}
         } m]
    {:account/id          id
     :account/version     version
     :account/insert_time insert-time
     :account/update_time update-time
     :account/username    username}))

(defn model->response [m]
  (let [{:account/keys [id username]} m]
    {:account.manager/id       id
     :account.manager/username username}))

;; get

(defn get-by-id [req]
  (log/info "get" req)
  (let [id (:account.manager/id req)
        model (dao/get-by-id {:account/id id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret (s/or :ok ::resp :err nil?))

;; create

(defn create [req]
  (log/info "create" req)
  (let [errors (specs/errors ::create-req req)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-manager-create
                         :reason  :invalid-spec
                         :details errors})))
      (-> req
          (create-req->model)
          (dao/insert)
          (model->response)))))

(s/fdef create
        :args (s/cat :req ::create-req)
        :ret ::resp)

;; delete

(defn delete [req]
  (log/info "delete" req)
  (let [id (:account.manager/id req)]
    (dao/delete {:account/id id})))

(s/fdef get-by-id
        :args (s/cat :req ::delete-req)
        :ret (s/or :ok ::resp :err nil?))