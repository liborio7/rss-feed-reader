(ns rss-feed-reader.domain.job
  (:require [rss-feed-reader.data.job :as dao]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [rss-feed-reader.utils.spec :as specs])
  (:import (java.util UUID)))

;; model

(s/def :job.domain/id uuid?)
(s/def :job.domain/version nat-int?)
(s/def :job.domain/order-id nat-int?)
(s/def :job.domain/insert-time inst?)
(s/def :job.domain/update-time inst?)
(s/def :job.domain/name string?)
(s/def :job.domain/execution-payload map?)
(s/def :job.domain/last-execution-payload map?)
(s/def :job.domain/last-execution-ms pos-int?)
(s/def :job.domain/description string?)
(s/def :job.domain/enabled boolean?)
(s/def :job.domain/locked boolean?)

(s/def ::model (s/keys :req [:job.domain/id
                             :job.domain/order-id
                             :job.domain/name
                             :job.domain/execution-payload
                             :job.domain/last-execution-payload
                             :job.domain/last-execution-ms
                             :job.domain/description
                             :job.domain/enabled
                             :job.domain/locked]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:job/keys [id order_id name execution_payload last_execution_payload last_execution_ms description enabled locked]} model]
    {:job.domain/id                     id
     :job.domain/order-id               order_id
     :job.domain/name                   name
     :job.domain/execution-payload      execution_payload
     :job.domain/last-execution-payload last_execution_payload
     :job.domain/last-execution-ms      last_execution_ms
     :job.domain/description            description
     :job.domain/enabled                enabled
     :job.domain/locked                 locked}))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:job.domain/id model)
        data-model (dao/get-by-id {:job/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:job.domain/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-name [model]
  (log/info "get by name" model)
  (let [name (:job.domain/name model)
        data-model (dao/get-by-name {:job/name name})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-name
        :args (s/cat :model (s/keys :req [:job.domain/name]))
        :ret (s/or :ok ::model :err nil?))

;; create

(s/def ::create-model (s/keys :req [:job.domain/name]
                              :opt [:job.domain/id
                                    :job.domain/version
                                    :job.domain/order-id
                                    :job.domain/insert-time
                                    :job.domain/update-time
                                    :job.domain/execution-payload
                                    :job.domain/description
                                    :job.domain/enabled
                                    :job.domain/locked]))

(defn domain-create-model->data-model [model]
  (let [now (t/now)
        {:job.domain/keys [id version order-id insert-time update-time name execution-payload description enabled locked]
         :or              {id          (UUID/randomUUID)
                           version     0
                           order-id    (tc/to-long now)
                           insert-time now
                           enabled     true
                           locked      false}
         } model]
    {:job/id                id
     :job/version           version
     :job/order_id          order-id
     :job/insert_time       insert-time
     :job/update_time       update-time
     :job/name              name
     :job/execution_payload execution-payload
     :job/description       description
     :job/enabled           enabled
     :job/locked            locked}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :job-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [job (get-by-name model)]
        job
        (-> model
            (domain-create-model->data-model)
            (dao/insert)
            (data-model->domain-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; update

(defn track_last_execution [model]
  (log/info "track last execution" model)
  (let [{:job.domain/keys [id version update-time last-execution-payload last-execution-ms]
         :or              {update-time (t/now)}} model]
    (-> {:job/id                     id
         :job/version                version
         :job/update_time            update-time
         :job/last-execution-payload last-execution-payload
         :job/last-execution-ms      last-execution-ms}
        (dao/update)
        (data-model->domain-model))))

(s/fdef track_last_execution
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]
                                    :opt [:job.domain/update-time
                                          :job.domain/last-execution-payload
                                          :job.domain/last-execution-ms]))
        :ret (s/or :ok ::model :err nil?))

(defn lock [model]
  (log/info "lock" model)
  (let [{:job.domain/keys [id version update-time]
         :or              {update-time (t/now)}} model]
    (-> {:job/id          id
         :job/version     version
         :job/update_time update-time
         :job/locked      true}
        (dao/update)
        (data-model->domain-model))))

(s/fdef lock
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]
                                    :opt [:job.domain/update-time]))
        :ret (s/or :ok ::model :err nil?))

(defn unlock [model]
  (log/info "unlock" model)
  (let [{:job.domain/keys [id version update-time]
         :or              {update-time (t/now)}} model]
    (-> {:job/id          id
         :job/version     version
         :job/update_time update-time
         :job/locked      false}
        (dao/update)
        (data-model->domain-model))))

(s/fdef unlock
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]
                                    :opt [:job.domain/update-time]))
        :ret (s/or :ok ::model :err nil?))