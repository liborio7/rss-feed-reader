(ns rss-feed-reader.core.account.feed.logic
  (:require [rss-feed-reader.core.account.feed.dao :as dao]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.account.logic :as account-logic]
            [rss-feed-reader.utils.spec :as specs]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

;; model

(s/def :account.feed.logic/id uuid?)
(s/def :account.feed.logic/version nat-int?)
(s/def :account.feed.logic/order-id nat-int?)
(s/def :account.feed.logic/insert-time inst?)
(s/def :account.feed.logic/update-time (s/nilable inst?))
(s/def :account.feed.logic/account (s/keys :req [:account.logic/id]))
(s/def :account.feed.logic/feed (s/keys :req [:feed.logic/id]))

(s/def ::model (s/keys :req [:account.feed.logic/id
                             :account.feed.logic/version
                             :account.feed.logic/order-id
                             :account.feed.logic/account
                             :account.feed.logic/feed]))

;; conversion

(defn dao-model->logic-model [model]
  (let [{:account.feed/keys [id version order_id account_id feed_id]} model
        account (account-logic/get-by-id {:account.logic/id account_id})
        feed (feed-logic/get-by-id {:feed.logic/id feed_id})]
    {:account.feed.logic/id       id
     :account.feed.logic/version  version
     :account.feed.logic/order-id order_id
     :account.feed.logic/account  account
     :account.feed.logic/feed     feed}))

;; get

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:account.feed.logic/id model)
        dao-model (dao/get-by-id {:account.feed/id id})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:account.feed.logic/id]))
        :ret (s/or :ok ::model :not-found nil?))

(defn get-by-account [model & {:keys [starting-after limit]
                               :or   {starting-after 0 limit 20}}]
  (log/debug "get by account" model "starting after" starting-after "limit" limit)
  (let [account-id (:account.logic/id (:account.feed.logic/account model))
        dao-models (dao/get-by-account-id {:account.feed/account_id account-id}
                                           :starting-after starting-after
                                           :limit limit)]
    (map dao-model->logic-model dao-models)))

(s/fdef get-by-account
        :args (s/cat :model (s/keys :req [:account.feed.logic/account])
                     :opts (s/* (s/cat :opt keyword? :val nat-int?)))
        :ret (s/coll-of ::model))

(defn get-by-account-and-feed [model]
  (log/debug "get by account and feed" model)
  (let [account-id (:account.logic/id (:account.feed.logic/account model))
        feed-id (:feed.logic/id (:account.feed.logic/feed model))
        dao-model (dao/get-by-account-id-and-feed-id {:account.feed/account_id account-id
                                                       :account.feed/feed_id    feed-id})]
    (if-not (nil? dao-model)
      (dao-model->logic-model dao-model))))

(s/fdef get-by-account-and-feed
        :args (s/cat :model (s/keys :req [:account.feed.logic/account
                                          :account.feed.logic/feed]))
        :ret (s/or :ok ::model :not-found nil?))

;; create

(s/def ::create-model (s/keys :req [:account.feed.logic/account
                                    :account.feed.logic/feed]
                              :opt [:account.feed.logic/id
                                    :account.feed.logic/version
                                    :account.feed.logic/order-id
                                    :account.feed.logic/insert-time
                                    :account.feed.logic/update-time]))

(defn logic-create-model->dao-model [model]
  (let [now (t/now)
        {:account.feed.logic/keys [id version order-id insert-time update-time account feed]
         :or                      {id          (UUID/randomUUID)
                                   version     0
                                   order-id    (tc/to-long now)
                                   insert-time now}
         } model]
    {:account.feed/id          id
     :account.feed/version     version
     :account.feed/order_id    order-id
     :account.feed/insert_time insert-time
     :account.feed/update_time update-time
     :account.feed/account_id  (:account.logic/id account)
     :account.feed/feed_id     (:feed.logic/id feed)}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-feed-logic-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [account (:account.feed.logic/account model)
            feed (:account.feed.logic/feed model)
            account-feed (get-by-account-and-feed {:account.feed.logic/account account
                                                   :account.feed.logic/feed    feed})]
        (if-not (nil? account-feed)
          account-feed
          (-> model
              (logic-create-model->dao-model)
              (dao/insert)
              (dao-model->logic-model)))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:account.feed.logic/id model)]
    (dao/delete {:account.feed/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:account.feed.logic/id]))
        :ret int?)