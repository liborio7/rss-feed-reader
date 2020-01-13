(ns rss-feed-reader.domain.account_feed
  (:require [rss-feed-reader.data.account_feed :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.account :as account-mgr]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; model

(s/def :account.feed.domain/id uuid?)
(s/def :account.feed.domain/version pos-int?)
(s/def :account.feed.domain/order-id pos-int?)
(s/def :account.feed.domain/insert-time inst?)
(s/def :account.feed.domain/update-time inst?)
(s/def :account.feed.domain/account (s/keys :req [:account.domain/id]))
(s/def :account.feed.domain/feed (s/keys :req [:feed.domain/id]))

(s/def ::model (s/keys :req [:account.feed.domain/id
                             :account.feed.domain/order-id
                             :account.feed.domain/account
                             :account.feed.domain/feed]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:account.feed/keys [id order_id account_id feed_id]} model
        account (account-mgr/get-by-id {:account.domain/id account_id})
        feed (feed-mgr/get-by-id {:feed.domain/id feed_id})]
    {:account.feed.domain/id       id
     :account.feed.domain/order-id order_id
     :account.feed.domain/account  account
     :account.feed.domain/feed     feed}))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:account.feed.domain/id model)
        data-model (dao/get-by-id {:account.feed/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account.feed.domain/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-account [model & {:keys [starting-after limit]
                               :or   {starting-after 0 limit 20}}]
  (log/info "get by account" model "starting after" starting-after "limit" limit)
  (let [account-id (:account.domain/id (:account.feed.domain/account model))
        data-models (dao/get-by-account-id {:account.feed/account_id account-id}
                                           :starting-after starting-after
                                           :limit limit)]
    (map data-model->domain-model data-models)))

(s/fdef get-by-account
        :args (s/cat :model (s/keys :req [:account.feed.domain/account])
                     :starting-after :account.feed.domain/order-id
                     :limit (s/int-in 0 100))
        :ret (s/or :ok ::model :err empty?))

(defn get-by-account-and-feed [model]
  (log/info "get by account and feed" model)
  (let [account-id (:account.domain/id (:account.feed.domain/account model))
        feed-id (:feed.domain/id (:account.feed.domain/feed model))
        data-model (dao/get-by-account-id-and-feed-id {:account.feed/account_id account-id
                                                       :account.feed/feed_id    feed-id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-account-and-feed
        :args (s/cat :model (s/keys :req [:account.feed.domain/account
                                          :account.feed.domain/feed]))
        :ret (s/or :ok ::model :err nil?))

;; create

(s/def ::create-model (s/keys :req [:account.feed.domain/account
                                    :account.feed.domain/feed]
                              :opt [:account.feed.domain/id
                                    :account.feed.domain/version
                                    :account.feed.domain/order-id
                                    :account.feed.domain/insert-time
                                    :account.feed.domain/update-time]))

(defn domain-create-model->data-model [model]
  (let [now (t/now)
        {:account.feed.domain/keys [id version order-id insert-time update-time account feed]
         :or                       {id          (java.util.UUID/randomUUID)
                                    version     0
                                    order-id    (tc/to-long now)
                                    insert-time now}
         } model]
    {:account.feed/id          id
     :account.feed/version     version
     :account.feed/order_id    order-id
     :account.feed/insert_time insert-time
     :account.feed/update_time update-time
     :account.feed/account_id  (:account.domain/id account)
     :account.feed/feed_id     (:feed.domain/id feed)}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-feed-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [account (:account.feed.domain/account model)
            feed (:account.feed.domain/feed model)
            account-feed (get-by-account-and-feed {:account.feed.domain/account account
                                                   :account.feed.domain/feed    feed})]
        (if-not (nil? account-feed)
          account-feed
          (-> model
              (domain-create-model->data-model)
              (dao/insert)
              (data-model->domain-model)))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:account.feed.domain/id model)]
    (dao/delete {:account.feed/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account.feed.domain/id]))
        :ret (s/or :ok ::model :err nil?))