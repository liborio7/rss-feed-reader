(ns rss-feed-reader.domain.account
  (:require [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [rss-feed-reader.utils.time :as time]
            [clojure.tools.logging :as log]
            [clojure.spec.alpha :as spec])
  (:import (java.util UUID)))

(def table :account)

;; model

(spec/def :account.domain/id uuid?)
(spec/def :account.domain/version nat-int?)
(spec/def :account.domain/order-id nat-int?)
(spec/def :account.domain/insert-time ::time/time)
(spec/def :account.domain/update-time (spec/nilable ::time/time))
(spec/def :account.domain/username (spec/nilable string?))
(spec/def :account.domain/chat-id int?)

(spec/def ::model (spec/keys :req [:account.domain/id
                                   :account.domain/version
                                   :account.domain/order-id
                                   :account.domain/insert-time
                                   :account.domain/update-time
                                   :account.domain/username
                                   :account.domain/chat-id]))

(spec/def ::create-model (spec/keys :req [:account.domain/chat-id]
                                    :opt [:account.domain/id
                                          :account.domain/username]))

;; conversion

(defn model->db [model]
  (let [now (time/instant-now)
        {:account.domain/keys [id username chat-id]
         :or                  {id (UUID/randomUUID)}} model]
    {:account/id          id
     :account/version     0
     :account/order-id    (time/instant->long now)
     :account/insert-time now
     :account/update-time nil
     :account/username    username
     :account/chat-id     chat-id}))

(defn db->model [model]
  (let [{:account/keys [id version order-id insert-time update-time username chat-id]} model]
    {:account.domain/id          id
     :account.domain/version     version
     :account.domain/order-id    order-id
     :account.domain/insert-time insert-time
     :account.domain/update-time update-time
     :account.domain/username    username
     :account.domain/chat-id     chat-id}))

;; component

(defprotocol Accounts
  (get-all [this])
  (get-by-id [this model])
  (get-by-ids [this models])
  (get-by-chat-id [this model])
  (create! [this model])
  (delete! [this model]))

(defrecord DbAccounts [datasource]
  Accounts

  ;; get

  (get-all [_]
    (log/debug "get all")
    (db/with-connection [conn datasource]
      (let [db-models (db/select-values conn table {})]
        (map db->model db-models))))

  (get-by-id [_ model]
    (log/debug "get by id" model)
    (db/with-connection [conn datasource]
      (let [id (:account.domain/id model)
            db-models (db/select conn table {:where [:= :account/id id]})]
        (when db-models
          (db->model db-models)))))

  (get-by-ids [_ models]
    (log/debug "get by" (count models) "ids")
    (db/with-connection [conn datasource]
      (let [ids (map :account.domain/id models)
            db-models (when (not-empty ids)
                        (db/select-values conn table {:where [:in :account/id ids]}))]
        (map db->model db-models))))

  (get-by-chat-id [_ model]
    (log/debug "get by chat id" model)
    (db/with-connection [conn datasource]
      (let [chat-id (:account.domain/chat-id model)
            db-model (db/select conn table {:where [:= :account/chat-id chat-id]})]
        (when db-model
          (db->model db-model)))))

  ;; create

  (create! [this model]
    (log/info "create" model)
    (let [errors (specs/errors ::create-model model)]
      (if (not-empty errors)
        (do
          (log/info "invalid request" errors)
          (throw (ex-info "invalid request"
                          {:cause   :account-create
                           :reason  :invalid-spec
                           :details errors})))
        (if-let [account (get-by-chat-id this model)]
          account
          (db/with-transaction [tx datasource]
            (->> model
                 (model->db)
                 (db/insert! tx table)
                 (db->model)))))))

  ;; delete

  (delete! [_ model]
    (log/info "delete" model)
    (db/with-transaction [tx datasource]
      (let [id (:account.domain/id model)]
        (db/delete! tx table {:where [:= :account/id id]})))))
