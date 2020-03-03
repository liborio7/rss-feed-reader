(ns rss-feed-reader.core.feed.logic
  (:require [rss-feed-reader.core.feed.dao :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log])
  (:import (java.util UUID)))

;; model

(s/def :feed.logic/id uuid?)
(s/def :feed.logic/version nat-int?)
(s/def :feed.logic/order-id nat-int?)
(s/def :feed.logic/insert-time inst?)
(s/def :feed.logic/update-time inst?)
(s/def :feed.logic/link uri?)

(s/def ::model (s/keys :req [:feed.logic/id
                             :feed.logic/version
                             :feed.logic/order-id
                             :feed.logic/link]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:feed/keys [id version order_id link]} model]
    {:feed.logic/id       id
     :feed.logic/version  version
     :feed.logic/order-id order_id
     :feed.logic/link     (uris/from-string link)}))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:feed.logic/id model)
        data-model (dao/get-by-id {:feed/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed.logic/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-link [model]
  (log/info "get by link" model)
  (let [link (str (:feed.logic/link model))
        data-model (dao/get-by-link {:feed/link link})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed.logic/link]))
        :ret (s/or :ok ::model :err nil?))

(defn get-all [& {:keys [starting-after limit]
                  :or   {starting-after 0 limit 20}}]
  (log/info "get all starting after" starting-after "limit" limit)
  (let [data-models (dao/get-all :starting-after starting-after :limit limit)]
    (map data-model->domain-model data-models)))

(s/fdef get-all
        :args (s/* (s/cat :opt keyword? :val nat-int?))
        :ret (s/or :ok (s/coll-of ::model) :err nil?))

;; create

(s/def ::create-model (s/keys :req [:feed.logic/link]
                              :opt [:feed.logic/id
                                    :feed.logic/version
                                    :feed.logic/order-id
                                    :feed.logic/insert-time
                                    :feed.logic/update-time]))

(defn domain-create-model->data-model [model]
  (let [now (t/now)
        {:feed.logic/keys [id version order-id insert-time update-time link]
         :or               {id          (UUID/randomUUID)
                            version     0
                            order-id    (tc/to-long now)
                            insert-time now
                            }
         } model]
    {:feed/id          id
     :feed/version     version
     :feed/order_id    order-id
     :feed/insert_time (tc/to-long insert-time)
     :feed/update_time (tc/to-long update-time)
     :feed/link        (str link)}))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed (get-by-link model)]
        feed
        (-> model
            (domain-create-model->data-model)
            (dao/insert)
            (data-model->domain-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:feed.logic/id model)]
    (dao/delete {:feed/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed.logic/id]))
        :ret int?)