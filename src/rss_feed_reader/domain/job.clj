(ns rss-feed-reader.domain.job
  (:require [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as t]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db])
  (:import (java.util UUID)))

(def table :job)

;; model

(s/def :job.domain/id uuid?)
(s/def :job.domain/version nat-int?)
(s/def :job.domain/order-id nat-int?)
(s/def :job.domain/insert-time ::t/time)
(s/def :job.domain/update-time (s/nilable ::t/time))
(s/def :job.domain/name string?)
(s/def :job.domain/execution-payload (s/nilable (s/map-of keyword? (s/or :int int? :string string?))))
(s/def :job.domain/last-execution-payload (s/nilable (s/map-of keyword? (s/or :int int? :string string?))))
(s/def :job.domain/last-execution-ms (s/nilable nat-int?))
(s/def :job.domain/description (s/nilable string?))
(s/def :job.domain/enabled boolean?)
(s/def :job.domain/locked boolean?)

(s/def ::model (s/keys :req [:job.domain/id
                             :job.domain/version
                             :job.domain/order-id
                             :job.domain/insert-time
                             :job.domain/update-time
                             :job.domain/name
                             :job.domain/execution-payload
                             :job.domain/last-execution-payload
                             :job.domain/last-execution-ms
                             :job.domain/description
                             :job.domain/enabled
                             :job.domain/locked]))

(s/def ::create-model (s/keys :req [:job.domain/name]
                              :opt [:job.domain/id
                                    :job.domain/execution-payload
                                    :job.domain/description
                                    :job.domain/enabled]))

(s/def ::update-model (s/keys :opt [:job.domain/name
                                    :job.domain/execution-payload
                                    :job.domain/description
                                    :job.domain/enabled]))

;; conversion

(defn model->db [model]
  (let [now (t/instant-now)
        {:job.domain/keys [id name execution-payload last-execution-payload last-execution-ms description enabled locked]
         :or              {id      (UUID/randomUUID)
                           enabled true
                           locked  false}} model]
    {:job/id                     id
     :job/version                0
     :job/order_id               (t/instant->long now)
     :job/insert_time            now
     :job/update_time            nil
     :job/name                   name
     :job/execution_payload      execution-payload
     :job/last_execution_payload last-execution-payload
     :job/last_execution_ms      last-execution-ms
     :job/description            description
     :job/enabled                enabled
     :job/locked                 locked}))

(defn- db->model [model]
  (let [{:job/keys [id version order_id insert_time update_time name execution_payload last_execution_payload last_execution_ms description enabled locked]} model]
    {:job.domain/id                     id
     :job.domain/version                version
     :job.domain/order-id               order_id
     :job.domain/insert-time            insert_time
     :job.domain/update-time            update_time
     :job.domain/name                   name
     :job.domain/execution-payload      execution_payload
     :job.domain/last-execution-payload last_execution_payload
     :job.domain/last-execution-ms      last_execution_ms
     :job.domain/description            description
     :job.domain/enabled                enabled
     :job.domain/locked                 locked}))

;; get

(defn get-all []
  (log/debug "get all")
  (let [db-models (db/select-values table {})]
    (map db->model db-models)))

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:job.domain/id model)
        db-model (db/select table {:where [:= :job/id id]})]
    (when db-model
      (db->model db-model))))

(defn get-by-name [model]
  (log/debug "get by name" model)
  (let [name (:job.domain/name model)
        db-model (db/select table {:where [:= :job/name name]})]
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
                        {:cause   :job-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [job (get-by-name model)]
        job
        (->> model
             (model->db)
             (db/insert! table)
             (db->model))))))

;; update

(defn update! [old-model new-model]
  (log/debug "update" old-model "with" new-model)
  (let [errors (specs/errors ::update-model new-model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :job-update
                         :reason  :invalid-spec
                         :details errors})))
      (let [{:job.domain/keys [id version]} old-model
            new-model (-> (merge old-model new-model)
                          (model->db)
                          (merge {:job/version     (inc version)
                                  :job/update_time (t/instant-now)}))]
        (->> new-model
             (db/update! table {:where [:and
                                            [:= :job/id id]
                                            [:= :job/version version]]})
             (db->model))))))

(defn track-last-execution! [model last-execution-payload last-execution-ms]
  (log/info "track last execution" model last-execution-payload last-execution-ms)
  (update! model {:job.domain/last-execution-payload last-execution-payload
                  :job.domain/last-execution-ms      last-execution-ms}))

(defn toggle-lock! [model locked]
  (log/info "lock" model locked)
  (update! model {:job.domain/locked locked}))

;; delete

(defn delete! [model]
  (log/info "delete" model)
  (let [id (:job.domain/id model)]
    (db/delete! table {:where [:= :job/id id]})))