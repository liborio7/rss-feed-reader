(ns rss-feed-reader.domain.account-feed
  (:require [rss-feed-reader.db.postgres :refer [ds]]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.domain.account :as accounts]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.sql :as sql]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.utils.time :as t]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

(def table :account_feed)

;; model

(s/def :account.feed.domain/id uuid?)
(s/def :account.feed.domain/version nat-int?)
(s/def :account.feed.domain/order-id nat-int?)
(s/def :account.feed.domain/insert-time ::t/time)
(s/def :account.feed.domain/update-time (s/nilable ::t/time))
(s/def :account.feed.domain/account :rss-feed-reader.domain.account/model)
(s/def :account.feed.domain/feed :rss-feed-reader.domain.feed/model)

(s/def ::model (s/keys :req [:account.feed.domain/id
                             :account.feed.domain/version
                             :account.feed.domain/order-id
                             :account.feed.domain/account
                             :account.feed.domain/feed]))

(s/def ::create-model (s/keys :req [:account.feed.domain/account
                                    :account.feed.domain/feed]
                              :opt [:account.feed.domain/id]))

;; conversion

(defn model->db [model]
  (let [now (t/instant-now)
        {:account.feed.domain/keys [id account feed]
         :or                       {id (UUID/randomUUID)}} model]
    {:account.feed/id          id
     :account.feed/version     0
     :account.feed/order_id    (t/instant->long now)
     :account.feed/insert_time now
     :account.feed/update_time nil
     :account.feed/account_id  (:account.domain/id account)
     :account.feed/feed_id     (:feed.domain/id feed)}))

(defn db->model
  ([model]
   (let [{:account.feed/keys [account_id feed_id]} model
         account (accounts/get-by-id {:account.domain/id account_id})
         feed (feeds/get-by-id {:feed.domain/id feed_id})]
     (db->model model account feed)))
  ([model account feed]
   (let [{:account.feed/keys [id version order_id insert_time update_time]} model]
     {:account.feed.domain/id          id
      :account.feed.domain/version     version
      :account.feed.domain/order-id    order_id
      :account.feed.domain/insert-time insert_time
      :account.feed.domain/update-time update_time
      :account.feed.domain/account     account
      :account.feed.domain/feed        feed})))

;; get

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:account.feed.domain/id model)
        db-model (sql/select ds table {:where [:= :account.feed/id id]})]
    (when db-model
      (db->model db-model))))

(defn get-by-account [model & {:keys [starting-after limit]
                               :or   {starting-after 0 limit 20}}]
  (log/debug "get by account" model "starting after" starting-after "limit" limit)
  (let [account (:account.feed.domain/account model)
        account-id (:account.domain/id account)
        db-models (sql/paginate
                    #(sql/select-values ds table {:where    [:and
                                                             [:= :account.feed/account_id account-id]
                                                             [:> :account.feed/order_id %]]
                                                  :order-by [[:account.feed/order_id :asc]]
                                                  :limit    20})
                    :account.feed/order_id
                    starting-after)
        feed-by-feed-id (->> db-models
                             (map :account.feed/feed_id)
                             (map (partial hash-map :feed.domain/id))
                             (feeds/get-by-ids)
                             (mapcat (juxt :feed.domain/id identity))
                             (apply hash-map))
        db->model* (fn [db-model]
                     (let [feed (get feed-by-feed-id (:account.feed/feed_id db-model))]
                       (db->model db-model account feed)))]
    (map db->model* db-models)))

(defn get-by-feed [model & {:keys [starting-after limit]
                            :or   {starting-after 0 limit 20}}]
  (log/debug "get by feed" model "starting after" starting-after "limit" limit)
  (let [feed (:account.feed.domain/feed model)
        feed-id (:feed.domain/id feed)
        db-models (sql/paginate
                    #(sql/select-values ds table {:where    [:and
                                                             [:= :account.feed/feed_id feed-id]
                                                             [:> :account.feed/order_id %]]
                                                  :order-by [[:account.feed/order_id :asc]]
                                                  :limit    20})
                    :account.feed/order_id
                    starting-after)
        account-by-account-id (->> db-models
                                   (map :account.feed/account_id)
                                   (map (partial hash-map :account.domain/id))
                                   (accounts/get-by-ids)
                                   (mapcat (juxt :account.domain/id identity))
                                   (apply hash-map))
        db->model* (fn [db-model]
                     (let [account (get account-by-account-id (:account.feed/account_id db-model))]
                       (db->model db-model account feed)))]
    (map db->model* db-models)))

(defn get-by-account-and-feed [model]
  (log/debug "get by account and feed" model)
  (let [account-id (:account.domain/id (:account.feed.domain/account model))
        feed-id (:feed.domain/id (:account.feed.domain/feed model))
        db-model (sql/select ds table {:where [:and
                                               [:= :account.feed/account_id account-id]
                                               [:= :account.feed/feed_id feed-id]]})]
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
                        {:cause   :account-feed-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [account (:account.feed.domain/account model)
            feed (:account.feed.domain/feed model)
            account-feed (get-by-account-and-feed {:account.feed.domain/account account
                                                   :account.feed.domain/feed    feed})]
        (if account-feed
          account-feed
          (->> model
               (model->db)
               (sql/insert! ds table)
               (db->model)))))))

;; delete

(defn delete! [model]
  (log/info "delete" model)
  (let [id (:account.feed.domain/id model)]
    (sql/delete! ds table {:where [:= :account.feed/id id]})))
