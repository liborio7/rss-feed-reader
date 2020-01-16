(ns rss-feed-reader.data.job
  (:refer-clojure :exclude [update])
  (:require [rss-feed-reader.data.postgres.db :as db]
            [rss-feed-reader.utils.sql :as sql]
            [clojure.spec.alpha :as s]))

;; utils

(def db db/connection)
(def table :job)

;; model

(s/def :job/id uuid?)
(s/def :job/version pos-int?)
(s/def :job/order_id pos-int?)
(s/def :job/insert_time inst?)
(s/def :job/update_time inst?)
(s/def :job/name string?)
(s/def :job/execution_payload map?)
(s/def :job/last_execution_payload map?)
(s/def :job/last_execution_ms pos-int?)
(s/def :job/description string?)
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
        :ret ::model)

(defn get-by-name [model]
  (sql/get-by-query db table {:= [:job/name (:job/name model)]}))

(s/fdef get-by-name
        :args (s/cat :model (s/keys :req [:job/name]))
        :ret ::model)

;; insert

(defn insert [model]
  (sql/insert db table :job/id model))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

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
        :ret (s/or :ok ::model :err nil?))