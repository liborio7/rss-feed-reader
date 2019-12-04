(ns rss-feed-reader.data.feed
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

;; utils

(def db db/connection)
(def table "feed")
(def opts {:qualifier table})

;; spec

(s/def :feed/id uuid?)
(s/def :feed/version int?)
(s/def :feed/insert_time inst-ms)
(s/def :feed/update_time inst?)
(s/def :feed/link string?)

(s/def ::model (s/keys :req [:feed/id
                             :feed/version
                             :feed/insert_time
                             :feed/update_time
                             :feed/link]))

;; get

(defn get-by-id [{:feed/keys [id]}]
  (jdbc/get-by-id db table id :feed/id opts))

(s/fdef get-by-id
        :args (s/cat :id :feed/id)
        :ret ::model)

(defn get-by-link [{:feed/keys [link]}]
  (-> (jdbc/find-by-keys db table {:feed/link link} opts)
      (first)))

(s/fdef get-by-link
        :args (s/cat :link :feed/link)
        :ret ::model)

;; insert

(defn insert [model]
  (log/info "insert" model)
  (let [affected-rows (jdbc/insert! db table model opts)]
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause   :feed-dao-insert
                       :reason  :no-rows-affected
                       :details [db table model]}))
      (first affected-rows))))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret ::model)

;; delete

(defn delete [{:feed/keys [id]}]
  (jdbc/delete! db table ["id = ?", id] opts))

(s/fdef delete
        :args (s/cat :id :feed/id)
        :ret (s/and int?))