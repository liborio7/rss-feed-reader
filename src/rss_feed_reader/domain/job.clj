(ns rss-feed-reader.domain.job
  (:require [clojure.spec.alpha :as spec]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as time]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db])
  (:import (java.util UUID)))

(def table :job)

;; model

(spec/def :job.domain/id uuid?)
(spec/def :job.domain/version nat-int?)
(spec/def :job.domain/order-id nat-int?)
(spec/def :job.domain/insert-time ::time/time)
(spec/def :job.domain/update-time (spec/nilable ::time/time))
(spec/def :job.domain/name string?)
(spec/def :job.domain/execution-payload (spec/nilable (spec/map-of keyword? (spec/or :int int? :string string?))))
(spec/def :job.domain/last-execution-payload (spec/nilable (spec/map-of keyword? (spec/or :int int? :string string?))))
(spec/def :job.domain/last-execution-ms (spec/nilable nat-int?))
(spec/def :job.domain/description (spec/nilable string?))
(spec/def :job.domain/enabled boolean?)
(spec/def :job.domain/locked boolean?)

(spec/def ::model (spec/keys :req [:job.domain/id
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

(spec/def ::create-model (spec/keys :req [:job.domain/name]
                                    :opt [:job.domain/id
                                          :job.domain/execution-payload
                                          :job.domain/description
                                          :job.domain/enabled]))

(spec/def ::update-model (spec/keys :opt [:job.domain/name
                                          :job.domain/execution-payload
                                          :job.domain/description
                                          :job.domain/enabled]))

;; conversion

(defn model->db [model]
  (let [now (time/instant-now)
        {:job.domain/keys [id name execution-payload last-execution-payload last-execution-ms description enabled locked]
         :or              {id      (UUID/randomUUID)
                           enabled true
                           locked  false}} model]
    {:job/id                     id
     :job/version                0
     :job/order-id               (time/instant->long now)
     :job/insert-time            now
     :job/update-time            nil
     :job/name                   name
     :job/execution-payload      execution-payload
     :job/last-execution-payload last-execution-payload
     :job/last-execution-ms      last-execution-ms
     :job/description            description
     :job/enabled                enabled
     :job/locked                 locked}))

(defn- db->model [model]
  (let [{:job/keys [id version order-id insert-time update-time name execution-payload last-execution-payload last-execution-ms description enabled locked]} model]
    {:job.domain/id                     id
     :job.domain/version                version
     :job.domain/order-id               order-id
     :job.domain/insert-time            insert-time
     :job.domain/update-time            update-time
     :job.domain/name                   name
     :job.domain/execution-payload      execution-payload
     :job.domain/last-execution-payload last-execution-payload
     :job.domain/last-execution-ms      last-execution-ms
     :job.domain/description            description
     :job.domain/enabled                enabled
     :job.domain/locked                 locked}))

;; component

(defprotocol Jobs
  (get-all [this])
  (get-by-id [this model])
  (get-by-name [this model])
  (create! [this model])
  (update! [this old-model new-model])
  (track-last-execution! [this model last-execution-payload last-execution-ms])
  (toggle-lock! [this model locked])
  (delete! [this model]))

(defrecord DbJobs [datasource]
  Jobs

  ;; get

  (get-all [_]
    (log/debug "get all")
    (db/with-transaction [conn datasource]
      (let [db-models (db/select-values conn table {})]
        (map db->model db-models))))

  (get-by-id [_ model]
    (log/debug "get by id" model)
    (db/with-transaction [conn datasource]
      (let [id (:job.domain/id model)
            db-model (db/select conn table {:where [:= :job/id id]})]
        (when db-model
          (db->model db-model)))))

  (get-by-name [_ model]
    (log/debug "get by name" model)
    (db/with-transaction [conn datasource]
      (let [name (:job.domain/name model)
            db-model (db/select conn table {:where [:= :job/name name]})]
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
                          {:cause   :job-create
                           :reason  :invalid-spec
                           :details errors})))
        (if-let [job (get-by-name this model)]
          job
          (db/with-transaction [tx datasource]
            (->> model
                 (model->db)
                 (db/insert! tx table)
                 (db->model)))))))

  ;; update

  (update! [_ old-model new-model]
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
                                    :job/update-time (time/instant-now)}))]
          (db/with-transaction [tx datasource]
            (->> new-model
                 (db/update! tx table {:where [:and
                                               [:= :job/id id]
                                               [:= :job/version version]]})
                 (db->model)))))))

  (track-last-execution! [this model last-execution-payload last-execution-ms]
    (log/info "track last execution" model last-execution-payload last-execution-ms)
    (update! this model {:job.domain/last-execution-payload last-execution-payload
                         :job.domain/last-execution-ms      last-execution-ms}))

  (toggle-lock! [this model locked]
    (log/info "lock" model locked)
    (update! this model {:job.domain/locked locked}))

  ;; delete

  (delete! [_ model]
    (log/info "delete" model)
    (db/with-transaction [tx datasource]
      (let [id (:job.domain/id model)]
        (db/delete! tx table {:where [:= :job/id id]})))))