(ns rss-feed-reader.domain.account
  (:require [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [rss-feed-reader.utils.time :as t]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s])
  (:import (java.util UUID)))

(def table :account)

;; model

(s/def :account.domain/id uuid?)
(s/def :account.domain/version nat-int?)
(s/def :account.domain/order-id nat-int?)
(s/def :account.domain/insert-time ::t/time)
(s/def :account.domain/update-time (s/nilable ::t/time))
(s/def :account.domain/username (s/nilable string?))
(s/def :account.domain/chat-id int?)

(s/def ::model (s/keys :req [:account.domain/id
                             :account.domain/version
                             :account.domain/order-id
                             :account.domain/insert-time
                             :account.domain/update-time
                             :account.domain/username
                             :account.domain/chat-id]))

(s/def ::create-model (s/keys :req [:account.domain/chat-id]
                              :opt [:account.domain/id
                                    :account.domain/username]))

;; conversion

(defn model->db [model]
  (let [now (t/instant-now)
        {:account.domain/keys [id username chat-id]
         :or                  {id (UUID/randomUUID)}} model]
    {:account/id          id
     :account/version     0
     :account/order_id    (t/instant->long now)
     :account/insert_time now
     :account/update_time nil
     :account/username    username
     :account/chat_id     chat-id}))

(defn db->model [model]
  (let [{:account/keys [id version order_id insert_time update_time username chat_id]} model]
    {:account.domain/id          id
     :account.domain/version     version
     :account.domain/order-id    order_id
     :account.domain/insert-time insert_time
     :account.domain/update-time update_time
     :account.domain/username    username
     :account.domain/chat-id     chat_id}))

;; get

(defn get-all []
  (log/debug "get all")
  (let [db-models (db/select-values table {})]
    (map db->model db-models)))

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:account.domain/id model)
        db-models (db/select table {:where [:= :account/id id]})]
    (when db-models
      (db->model db-models))))

(defn get-by-ids [models]
  (log/debug "get by" (count models) "ids")
  (let [ids (map :account.domain/id models)
        db-models (when (not-empty ids)
                    (db/select-values table {:where [:in :account/id ids]}))]
    (map db->model db-models)))

(defn get-by-chat-id [model]
  (log/debug "get by chat id" model)
  (let [chat-id (:account.domain/chat-id model)
        db-model (db/select table {:where [:= :account/chat_id chat-id]})]
    (when db-model
      (db->model db-model))))

;; create

(defn create! [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [account (get-by-chat-id model)]
        account
        (->> model
             (model->db)
             (db/insert! table)
             (db->model))))))

;; delete

(defn delete! [model]
  (log/info "delete" model)
  (let [id (:account.domain/id model)]
    (db/delete! table {:where [:= :account/id id]})))