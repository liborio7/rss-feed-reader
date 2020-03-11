(ns rss-feed-reader.core.job.dao
  (:refer-clojure :exclude [update])
  (:require [rss-feed-reader.db.postgres :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def db db/connection)
(def table :job)

;; model

(s/def :job/id uuid?)
(s/def :job/version nat-int?)
(s/def :job/order_id nat-int?)
(s/def :job/insert_time inst?)
(s/def :job/update_time (s/nilable inst?))
(s/def :job/name string?)
(s/def :job/execution_payload (s/nilable map?))
(s/def :job/last_execution_payload (s/nilable map?))
(s/def :job/last_execution_ms (s/nilable pos-int?))
(s/def :job/description (s/nilable string?))
(s/def :job/enabled boolean?)
(s/def :job/locked boolean?)

(s/def ::model (s/keys :req [:job/id
                             :job/version
                             :job/order_id
                             :job/insert_time
                             :job/update_time
                             :job/name
                             :job/execution_payload
                             :job/last_execution_payload
                             :job/last_execution_ms
                             :job/description
                             :job/enabled
                             :job/locked]))

;; get

(defn get-by-id [model]
  (sql/get-by-id db table :job/id model))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:job/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-name [model]
  (sql/get-by-query db table {:where [:= :job/name (:job/name model)]}))

(s/fdef get-by-name
        :args (s/cat :model (s/keys :req [:job/name]))
        :ret (s/or :ok ::model :not-found nil?))

;; insert

(defn insert [model]
  (sql/insert db table :job/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; update

(defn update [model]
  (sql/update db table :job/id :job/version model))

(s/fdef update
        :args (s/cat :model (s/keys :req [:job/id
                                          :job/version]
                                    :opt [:job/last_execution_payload
                                          :job/last_execution_ms
                                          :job/enabled
                                          :job/locked]))
        :ret ::model)