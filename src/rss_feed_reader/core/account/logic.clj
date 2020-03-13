(ns rss-feed-reader.core.account.logic
  (:require [rss-feed-reader.core.account.dao :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

;; model

(s/def :account.logic/id uuid?)
(s/def :account.logic/version nat-int?)
(s/def :account.logic/order-id nat-int?)
(s/def :account.logic/insert-time inst?)
(s/def :account.logic/update-time (s/nilable inst?))
(s/def :account.logic/username string?)

(s/def ::model (s/keys :req [:account.logic/id
                             :account.logic/version
                             :account.logic/order-id
                             :account.logic/username]))

;; conversion

(defn dao-model->logic-model [model]
  (let [{:account/keys [id version order_id username]} model]
    {:account.logic/id       id
     :account.logic/version  version
     :account.logic/order-id order_id
     :account.logic/username username}))

;; get

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:account.logic/id model)
        dao-model (dao/get-by-id {:account/id id})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account.logic/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-username [model]
  (log/debug "get by username" model)
  (let [username (:account.logic/username model)
        dao-model (dao/get-by-username {:account/username username})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-username
        :args (s/cat :model (s/keys :req [:account.logic/id]))
        :ret (s/or :ok ::model :not-found nil?))

;; create

(s/def ::create-model (s/keys :req [:account.logic/username]
                              :opt [:account.logic/id
                                    :account.logic/version
                                    :account.logic/order-id
                                    :account.logic/insert-time
                                    :account.logic/update-time]))

(defn logic-create-model->dao-model [model]
  (let [now (t/now)
        {:account.logic/keys [id version order-id insert-time update-time username]
         :or                 {id          (UUID/randomUUID)
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
                        {:cause   :account-logic-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [account (get-by-username model)]
        account
        (-> model
            (logic-create-model->dao-model)
            (dao/insert)
            (dao-model->logic-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:account.logic/id model)]
    (dao/delete {:account/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account.logic/id]))
        :ret int?)