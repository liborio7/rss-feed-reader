(ns rss-feed-reader.data.account
  (:require [rss-feed-reader.data.postgres.db :as db]
            [clojure.java.jdbc :as jdbc]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

;; utils

(def db db/connection)
(def table "account")
(def opts {:qualifier (clojure.string/replace table "_" ".")})

;; spec

(s/def :account/id uuid?)
(s/def :account/version int?)
(s/def :account/order_id int?)
(s/def :account/insert_time inst-ms)
(s/def :account/update_time inst?)
(s/def :account/username string?)

(s/def ::model (s/keys :req [:account/id
                             :account/version
                             :account/order_id
                             :account/insert_time
                             :account/update_time
                             :account/username]))

;; get by id

(defn get-by-id [{:account/keys [id]}]
  (log/info "get by id" id)
  (let [result (jdbc/get-by-id db table id :account/id opts)]
    (log/info "result" result)
    result))

(s/fdef get-by-id
        :args (s/cat :id :account/id)
        :ret ::model)

(defn get-by-username [{:account/keys [username]}]
  (log/info "get by username" username)
  (let [result (-> (jdbc/find-by-keys db table {:account/username username} opts)
                   (first))]
    (log/info "result" result)
    result))

(s/fdef get-by-username
        :args (s/cat :username :account/username)
        :ret ::model)

;; insert

(defn insert [model]
  (log/info "insert" model)
  (let [affected-rows (jdbc/insert! db table model opts)]
    (log/info "affected rows" affected-rows)
    (if (empty? affected-rows)
      (throw (ex-info "no rows has been inserted"
                      {:cause   :account-data-insert
                       :reason  :no-rows-affected
                       :details [db table model]}))
      (first affected-rows))))

(s/fdef insert
        :args (s/cat :model ::model)
        :ret (s/or :ok ::model :err nil?))

;; delete

(defn delete [{:account/keys [id]}]
  (log/info "delete" id)
  (let [affected-rows (jdbc/delete! db table ["id = ?", id] opts)]
    (log/info "affected rows" affected-rows)
    affected-rows))

(s/fdef delete
        :args (s/cat :id :account/id)
        :ret int?)