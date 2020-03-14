(ns rss-feed-reader.core.job.logic
  (:require [rss-feed-reader.core.job.dao :as dao]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [rss-feed-reader.utils.spec :as specs])
  (:import (java.util UUID)))

;; model

(s/def :job.logic/id uuid?)
(s/def :job.logic/version nat-int?)
(s/def :job.logic/order-id nat-int?)
(s/def :job.logic/insert-time inst?)
(s/def :job.logic/update-time (s/nilable inst?))
(s/def :job.logic/name string?)
(s/def :job.logic/execution-payload (s/nilable map?))
(s/def :job.logic/last-execution-payload (s/nilable map?))
(s/def :job.logic/last-execution-ms (s/nilable pos-int?))
(s/def :job.logic/description (s/nilable string?))
(s/def :job.logic/enabled boolean?)
(s/def :job.logic/locked boolean?)

(s/def ::model (s/keys :req [:job.logic/id
                             :job.logic/version
                             :job.logic/order-id
                             :job.logic/name
                             :job.logic/execution-payload
                             :job.logic/last-execution-payload
                             :job.logic/last-execution-ms
                             :job.logic/description
                             :job.logic/enabled
                             :job.logic/locked]))

;; conversion

(defn dao-model->logic-model [model]
  (let [{:job/keys [id version order_id name execution_payload last_execution_payload last_execution_ms description enabled locked]} model]
    {:job.logic/id                     id
     :job.logic/version                version
     :job.logic/order-id               order_id
     :job.logic/name                   name
     :job.logic/execution-payload      execution_payload
     :job.logic/last-execution-payload last_execution_payload
     :job.logic/last-execution-ms      last_execution_ms
     :job.logic/description            description
     :job.logic/enabled                enabled
     :job.logic/locked                 locked}))

;; get

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:job.logic/id model)
        dao-model (dao/get-by-id {:job/id id})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:job.logic/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-name [model]
  (log/debug "get by name" model)
  (let [name (:job.logic/name model)
        dao-model (dao/get-by-name {:job/name name})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-name
        :args (s/cat :model (s/keys :req [:job.logic/name]))
        :ret (s/or :ok ::model :not-found nil?))

;; create

(s/def ::create-model (s/keys :req [:job.logic/name]
                              :opt [:job.logic/id
                                    :job.logic/version
                                    :job.logic/order-id
                                    :job.logic/insert-time
                                    :job.logic/update-time
                                    :job.logic/execution-payload
                                    :job.logic/description
                                    :job.logic/enabled
                                    :job.logic/locked]))

(defn logic-create-model->dao-model [model]
  (let [now (t/now)
        {:job.logic/keys [id version order-id insert-time update-time name execution-payload description enabled locked]
         :or             {id          (UUID/randomUUID)
                          version     0
                          order-id    (tc/to-long now)
                          insert-time now
                          enabled     true
                          locked      false}
         } model]
    {:job/id                     id
     :job/version                version
     :job/order_id               order-id
     :job/insert_time            insert-time
     :job/update_time            update-time
     :job/name                   name
     :job/execution_payload      execution-payload
     :job/last_execution_payload nil
     :job/last_execution_ms      nil
     :job/description            description
     :job/enabled                enabled
     :job/locked                 locked}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :job-logic-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [job (get-by-name model)]
        job
        (-> model
            (logic-create-model->dao-model)
            (dao/insert)
            (dao-model->logic-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; update

(defn track_last_execution [model]
  (log/info "track last execution" model)
  (let [{:job.logic/keys [id version update-time last-execution-payload last-execution-ms]
         :or             {update-time (t/now)}} model]
    (-> {:job/id                     id
         :job/version                version
         :job/update_time            update-time
         :job/last-execution-payload last-execution-payload
         :job/last-execution-ms      last-execution-ms}
        (dao/update)
        (dao-model->logic-model))))

(s/fdef track_last_execution
        :args (s/cat :model (s/keys :req [:job.logic/id
                                          :job.logic/version]
                                    :opt [:job.logic/update-time
                                          :job.logic/last-execution-payload
                                          :job.logic/last-execution-ms]))
        :ret ::model)

(defn lock [model]
  (log/info "lock" model)
  (let [{:job.logic/keys [id version update-time]
         :or             {update-time (t/now)}} model]
    (-> {:job/id          id
         :job/version     version
         :job/update_time update-time
         :job/locked      true}
        (dao/update)
        (dao-model->logic-model))))

(s/fdef lock
        :args (s/cat :model (s/keys :req [:job.logic/id
                                          :job.logic/version]
                                    :opt [:job.logic/update-time]))
        :ret ::model)

(defn unlock [model]
  (log/info "unlock" model)
  (let [{:job.logic/keys [id version update-time]
         :or             {update-time (t/now)}} model]
    (-> {:job/id          id
         :job/version     version
         :job/update_time update-time
         :job/locked      false}
        (dao/update)
        (dao-model->logic-model))))

(s/fdef unlock
        :args (s/cat :model (s/keys :req [:job.logic/id
                                          :job.logic/version]
                                    :opt [:job.logic/update-time]))
        :ret ::model)