(ns rss-feed-reader.feed.dao
  (:require [rss-feed-reader.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

;; utils

(def db db/connection)
(def table "feeds")

;; spec

(s/def :feed/id uuid?)
(s/def :feed/version int?)
(s/def :feed/insert_time inst-ms)
(s/def :feed/update_time inst?)
(s/def :feed/title string?)
(s/def :feed/link string?)
(s/def :feed/description string?)

(s/def ::model (s/keys :req [:feed/id
                             :feed/version
                             :feed/insert_time
                             :feed/update_time
                             :feed/title
                             :feed/link]
                       :opt [:feed/description]))

;; insert

(defn insert [feed]
  (log/info "insert" feed)
  (let [affected-rows (jdbc/insert! db table feed {:qualifier "feed"})]
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause  :dao-insert
                       :reason :no-rows-affected}))
      (first affected-rows))))

(s/fdef insert
        :args (s/cat :feed ::model)
        :ret ::model)

;; get by id

(defn get-by-id [{:feed/keys [id]}]
  (jdbc/get-by-id db table id :feed/id {:qualifier "feed"}))

(s/fdef get-by-id
        :args (s/cat :id :feed/id)
        :ret ::model)

;; update

;; TODO

;; delete

(defn delete [{:feed/keys [id]}]
  (jdbc/delete! db table ["id = ?", id] {:qualifier "feed"}))

(s/fdef delete
        :args (s/cat :id :feed/id)
        :ret (s/and int?))