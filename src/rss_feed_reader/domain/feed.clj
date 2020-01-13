(ns rss-feed-reader.domain.feed
  (:require [rss-feed-reader.data.feed :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; model

(s/def :feed.domain/id uuid?)
(s/def :feed.domain/version pos-int?)
(s/def :feed.domain/order-id pos-int?)
(s/def :feed.domain/insert-time inst?)
(s/def :feed.domain/update-time inst?)
(s/def :feed.domain/link uri?)

(s/def ::model (s/keys :req [:feed.domain/id
                             :feed.domain/link
                             :feed.domain/order-id]))

;; conversion

(defn data-model->domain-model [model]
  (let [{:feed/keys [id order_id link]} model]
    {:feed.domain/id       id
     :feed.domain/order-id order_id
     :feed.domain/link     (uris/from-string link)}))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:feed.domain/id model)
        data-model (dao/get-by-id {:feed/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed.domain/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-link [model]
  (log/info "get by link" model)
  (let [link (str (:feed.domain/link model))
        data-model (dao/get-by-link {:feed/link link})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed.domain/link]))
        :ret (s/or :ok ::model :err nil?))

(defn get-all [& {:keys [starting-after limit]
                  :or   {starting-after 0 limit 20}}]
  (log/info "get all starting after" starting-after "limit" limit)
  (let [data-models (dao/get-all :starting-after starting-after :limit limit)]
    (map data-model->domain-model data-models)))

(s/fdef get-all
        :args (s/cat :starting-after :feed.domain/order-id
                     :limit (s/int-in 0 100))
        :ret (s/or :ok (s/coll-of ::model) :err nil?))

;; create

(s/def ::create-model (s/keys :req [:feed.domain/link]
                              :opt [:feed.domain/id
                                    :feed.domain/version
                                    :feed.domain/order-id
                                    :feed.domain/insert-time
                                    :feed.domain/update-time]))

(defn domain-create-model->data-model [model]
  (let [now (t/now)
        {:feed.domain/keys [id version order-id insert-time update-time link]
         :or               {id          (java.util.UUID/randomUUID)
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

(defn delete [req]
  (log/info "delete" req)
  (let [id (:feed.domain/id req)]
    (dao/delete {:feed/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed.domain/id]))
        :ret int?)