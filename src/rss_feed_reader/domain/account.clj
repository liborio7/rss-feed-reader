(ns rss-feed-reader.domain.account
  (:require [rss-feed-reader.dao.account :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

;; model

(s/def :account.domain/id uuid?)
(s/def :account.domain/version nat-int?)
(s/def :account.domain/order-id nat-int?)
(s/def :account.domain/insert-time inst?)
(s/def :account.domain/update-time inst?)
(s/def :account.domain/username string?)

(s/def ::model (s/keys :req [:account.domain/id
                             :account.domain/version
                             :account.domain/order-id
                             :account.domain/username]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:account/keys [id version order_id username]} model]
    {:account.domain/id       id
     :account.domain/version  version
     :account.domain/order-id order_id
     :account.domain/username username}))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:account.domain/id model)
        data-model (dao/get-by-id {:account/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account.domain/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-username [model]
  (log/info "get by username" model)
  (let [username (:account.domain/username model)
        data-model (dao/get-by-username {:account/username username})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-username
        :args (s/cat :model (s/keys :req [:account.domain/id]))
        :ret (s/or :ok ::rest :err nil?))

;; create

(s/def ::create-model (s/keys :req [:account.domain/username]
                              :opt [:account.domain/id
                                    :account.domain/version
                                    :account.domain/order-id
                                    :account.domain/insert-time
                                    :account.domain/update-time]))

(defn domain-create-model->data-model [model]
  (let [now (t/now)
        {:account.domain/keys [id version order-id insert-time update-time username]
         :or                  {id          (UUID/randomUUID)
                               version     0
                               order-id    (tc/to-long now)
                               insert-time now}
         } model]
    {:account/id          id
     :account/version     version
     :account/order_id    order-id
     :account/insert_time insert-time
     :account/update_time update-time
     :account/username    username}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [account (get-by-username model)]
        account
        (-> model
            (domain-create-model->data-model)
            (dao/insert)
            (data-model->domain-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:account.domain/id model)]
    (dao/delete {:account/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account.domain/id]))
        :ret (s/or :ok ::model :err nil?))