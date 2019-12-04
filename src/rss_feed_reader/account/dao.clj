(ns rss-feed-reader.account.dao
  (:require [rss-feed-reader.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

;; utils

(def db db/connection)
(def table "account")
(def opts {:qualifier table})

;; spec

(s/def :account/id uuid?)
(s/def :account/version int?)
(s/def :account/insert_time inst-ms)
(s/def :account/update_time inst?)
(s/def :account/username string?)

(s/def ::model (s/keys :req [:account/id
                             :account/version
                             :account/insert_time
                             :account/update_time
                             :account/username]))

;; get by id

(defn get-by-id [{:account/keys [id]}]
  (jdbc/get-by-id db table id :account/id opts))

(s/fdef get-by-id
        :args (s/cat :id :account/id)
        :ret ::model)

(defn get-by-username [{:account/keys [username]}]
  (-> (jdbc/find-by-keys db table {:account/username username} opts)
      (first)))

(s/fdef get-by-username
        :args (s/cat :link :account/username)
        :ret ::model)

;; insert

(defn insert [model]
  (log/info "insert" model)
  (let [affected-rows (jdbc/insert! db table model opts)]
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause   :account-dao-insert
                       :reason  :no-rows-affected
                       :details [db table model]}))
      (first affected-rows))))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [{:account/keys [id]}]
  (jdbc/delete! db table ["id = ?", id] opts))

(s/fdef delete
        :args (s/cat :id :account/id)
        :ret int?)