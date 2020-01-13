(ns rss-feed-reader.domain.job
  (:require [rss-feed-reader.data.job :as dao]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

;; model

(s/def :job.domain/id uuid?)
(s/def :job.domain/version pos-int?)
(s/def :job.domain/order_id pos-int?)
(s/def :job.domain/insert_time inst?)
(s/def :job.domain/update_time inst?)
(s/def :job.domain/name string?)
(s/def :job.domain/execution_payload map?)
(s/def :job.domain/last_execution_payload map?)
(s/def :job.domain/last_execution_ms pos-int?)
(s/def :job.domain/description string?)
(s/def :job.domain/enabled boolean?)
(s/def :job.domain/locked boolean?)

(s/def ::model (s/keys :req [:job.domain/id
                             :job.domain/order-id
                             :job.domain/name]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:job/keys [id order_id name execution_payload last_execution_payload last_execution_ms description enabled locked]} model]
    {:job.domain/id                     id
     :job.domain/order-id               order_id
     :job.domain/name                   name
     :job.domain/execution_payload      execution_payload
     :job.domain/last_execution_payload last_execution_payload
     :job.domain/last_execution_ms      last_execution_ms
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
        :ret (s/or :ok ::rest :err nil?))

;; update

(defn track_last_execution [model]
  (log/info "track last execution" model)
  (let [{:job.domain/keys [id version last_execution_payload last_execution_ms]} model]
    (-> {:job.domain/id                     id
         :job.domain/version                version
         :job.domain/last_execution_payload last_execution_payload
         :job.domain/last_execution_ms      last_execution_ms
         :job.domain/locked                 false}
        (dao/update)
        (data-model->domain-model))))

(s/fdef track_last_execution
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]
                                    :opt [:job.domain/last_execution_payload
                                          :job.domain/last_execution_ms]))
        :ret (s/or :ok ::rest :err nil?))

(defn lock [model]
  (log/info "lock" model)
  (let [{:job.domain/keys [id version]} model]
    (-> {:job.domain/id      id
         :job.domain/version version
         :job.domain/locked  true}
        (dao/update)
        (data-model->domain-model))))

(s/fdef lock
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]))
        :ret (s/or :ok ::rest :err nil?))

(defn unlock [model]
  (log/info "unlock" model)
  (let [{:job.domain/keys [id version]} model]
    (-> {:job.domain/id      id
         :job.domain/version version
         :job.domain/locked  false}
        (dao/update)
        (data-model->domain-model))))

(s/fdef unlock
        :args (s/cat :model (s/keys :req [:job.domain/id
                                          :job.domain/version]))
        :ret (s/or :ok ::rest :err nil?))