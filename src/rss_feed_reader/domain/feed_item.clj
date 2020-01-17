(ns rss-feed-reader.domain.feed_item
  (:require [rss-feed-reader.data.feed_item :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.uri :as uris]))

;; model

(s/def :feed.item.domain/id uuid?)
(s/def :feed.item.domain/version pos-int?)
(s/def :feed.item.domain/order-id pos-int?)
(s/def :feed.item.domain/insert-time inst?)
(s/def :feed.item.domain/update-time inst?)
(s/def :feed.item.domain/feed (s/keys :req [:feed.domain/id]))
(s/def :feed.item.domain/title string?)
(s/def :feed.item.domain/link uri?)
(s/def :feed.item.domain/pub-time inst?)
(s/def :feed.item.domain/description string?)

(s/def ::model (s/keys :req [:feed.item.domain/id
                             :feed.item.domain/order-id
                             :feed.item.domain/feed
                             :feed.item.domain/title
                             :feed.item.domain/link
                             :feed.item.domain/pub-time
                             :feed.item.domain/description]))

;; conversion

(defn data-model->domain-model
  ([model] (data-model->domain-model model (feed-mgr/get-by-id {:feed.domain/id (:feed.item/feed_id model)})))
  ([model feed]
   (let [{:feed.item/keys [id order_id title link pub-time description]} model]
     {:feed.item.domain/id          id
      :feed.item.domain/order-id    order_id
      :feed.item.domain/feed        feed
      :feed.item.domain/title       title
      :feed.item.domain/link        (uris/from-string link)
      :feed.item.domain/pub-time    (tc/from-long pub-time)
      :feed.item.domain/description description})))

;; get

(defn get-by-id [model]
  (log/info "get by id" model)
  (let [id (:feed.item.domain/id model)
        data-model (dao/get-by-id {:feed.item/id id})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed.item.domain/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-feed [model & {:keys [starting-after limit]
                            :or   {starting-after 0 limit 20}}]
  (log/info "get by feed" model "starting-after" starting-after "limit" limit)
  (let [feed (:feed.item.domain/feed model)
        feed-id (:feed.domain/id feed)
        data-models (dao/get-by-feed-id {:feed.item/feed_id feed-id}
                                        :starting-after starting-after
                                        :limit limit)]
    (map #(data-model->domain-model % feed) data-models)))

(s/fdef get-by-feed
        :args (s/cat :model (s/keys :req [:feed.item.domain/feed])
                     :starting-after :feed.item.domain/order-id
                     :limit (s/int-in 0 100))
        :ret (s/or :ok ::model :err empty?))

(defn get-by-link [model]
  (log/info "get by link" model)
  (let [link (str (:feed.item.domain/link model))
        data-model (dao/get-by-link {:feed.item/link link})]
    (if-not (nil? data-model)
      (data-model->domain-model data-model))))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed.item.domain/link]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-links [models]
  (log/info "get by" (count models) "links")
  (let [feeds-map (->> models
                       (group-by #(:feed.domain/id (:feed.item.domain/feed %)))
                       (map (fn [[k _]] [k (feed-mgr/get-by-id {:feed.domain/id k})]))
                       (into {}))
        links (->> models
                   (map :feed.item.domain/link)
                   (map str)
                   (map #(assoc {} :feed.item/link %))
                   (reduce conj []))
        data-models (dao/get-by-links links)]
    (map #(data-model->domain-model % (get feeds-map (:feed.item/feed_id %))) data-models)))

(s/fdef get-by-links
        :args (s/cat :model (s/coll-of (s/keys :req [:feed.item.domain/link])))
        :ret (s/or :ok (s/coll-of ::model) :err nil?))

;; create

(s/def ::create-model (s/keys :req [:feed.item.domain/feed
                                    :feed.item.domain/title
                                    :feed.item.domain/link
                                    :feed.item.domain/pub-time]
                              :opt [:feed.item.domain/id
                                    :feed.item.domain/version
                                    :feed.item.domain/order-id
                                    :feed.item.domain/insert-time
                                    :feed.item.domain/update-time
                                    :feed.item.domain/description]))

(defn domain-create-model->data-model
  ([model] (domain-create-model->data-model model (t/now)))
  ([model time]
   (let [{:feed.item.domain/keys [id version order-id insert-time update-time feed title link pub-time description]
          :or                    {id          (java.util.UUID/randomUUID)
                                  version     0
                                  order-id    (tc/to-long (:feed.item.domain/pub-time model))
                                  insert-time time}
          } model]
     {:feed.item/id          id
      :feed.item/version     version
      :feed.item/order_id    order-id
      :feed.item/insert_time insert-time
      :feed.item/update_time update-time
      :feed.item/feed_id     (:feed.domain/id feed)
      :feed.item/title       title
      :feed.item/link        (str link)
      :feed.item/pub_time    pub-time
      :feed.item/description description})))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed-item (get-by-link model)]
        feed-item
        (-> model
            (domain-create-model->data-model)
            (dao/insert)
            (data-model->domain-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

(s/def ::create-multi-models (s/coll-of ::create-model))

(defn domain-create-models->data-models [models]
  (let [now (t/now)]
    (->> models
         (map #(domain-create-model->data-model % now))
         (reduce conj []))))

(defn create-multi [models]
  (log/info "create" (count models) "models")
  (let [errors (specs/errors ::create-multi-models models)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-domain-create-multi
                         :reason  :invalid-spec
                         :details errors})))
      (let [feeds-map (->> models
                           (group-by #(:feed.domain/id (:feed.item.domain/feed %)))
                           (map (fn [[k _]] [k (feed-mgr/get-by-id {:feed.domain/id k})]))
                           (into {}))]
        (->> models
             (domain-create-models->data-models)
             (dao/insert-multi)
             (map #(data-model->domain-model % (get feeds-map (:feed.item/feed_id %))))
             (reduce conj []))))))

(s/fdef create-multi
        :args (s/cat :models ::create-multi-models)
        :ret (s/coll-of ::model))

;; delete

(defn delete [req]
  (log/info "delete" req)
  (let [id (:feed.item.domain/id req)]
    (dao/delete {:feed.item/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed.item.domain/id]))
        :ret (s/or :ok ::model :err nil?))