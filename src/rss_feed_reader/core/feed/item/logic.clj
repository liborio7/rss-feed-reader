(ns rss-feed-reader.core.feed.item.logic
  (:require [rss-feed-reader.core.feed.item.dao :as dao]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.utils.spec :as specs]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.uri :as uris])
  (:import (java.util UUID)))

;; model

(s/def :feed.item.logic/id uuid?)
(s/def :feed.item.logic/version nat-int?)
(s/def :feed.item.logic/order-id nat-int?)
(s/def :feed.item.logic/insert-time inst?)
(s/def :feed.item.logic/update-time (s/nilable inst?))
(s/def :feed.item.logic/feed (s/keys :req [:feed.logic/id]))
(s/def :feed.item.logic/title string?)
(s/def :feed.item.logic/link uri?)
(s/def :feed.item.logic/pub-time inst?)
(s/def :feed.item.logic/description (s/nilable string?))

(s/def ::model (s/keys :req [:feed.item.logic/id
                             :feed.item.logic/version
                             :feed.item.logic/order-id
                             :feed.item.logic/feed
                             :feed.item.logic/title
                             :feed.item.logic/link
                             :feed.item.logic/pub-time
                             :feed.item.logic/description]))

;; conversion

(defn dao-model->logic-model
  ([model] (dao-model->logic-model model (feed-logic/get-by-id {:feed.logic/id (:feed.item/feed_id model)})))
  ([model feed]
   (let [{:feed.item/keys [id version order_id title link pub-time description]} model]
     {:feed.item.logic/id          id
      :feed.item.logic/version     version
      :feed.item.logic/order-id    order_id
      :feed.item.logic/feed        feed
      :feed.item.logic/title       title
      :feed.item.logic/link        (uris/from-string link)
      :feed.item.logic/pub-time    (tc/from-long pub-time)
      :feed.item.logic/description description})))

;; get

(defn get-by-id [model]
  (log/debug "get by id" model)
  (let [id (:feed.item.logic/id model)
        data-model (dao/get-by-id {:feed.item/id id})]
    (if-not (nil? data-model)
      (dao-model->logic-model data-model))))

(s/fdef get-by-id
        :args (s/cat :model (s/keys :req [:feed.item.logic/id]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-feed [model & {:keys [starting-after limit]
                            :or   {starting-after 0 limit 20}}]
  (log/debug "get by feed" model "starting-after" starting-after "limit" limit)
  (let [feed (:feed.item.logic/feed model)
        feed-id (:feed.logic/id feed)
        data-models (dao/get-by-feed-id {:feed.item/feed_id feed-id}
                                        :starting-after starting-after
                                        :limit limit)]
    (map #(dao-model->logic-model % feed) data-models)))

(s/fdef get-by-feed
        :args (s/cat :model (s/keys :req [:feed.item.logic/feed])
                     :opts (s/* (s/cat :opt keyword? :val nat-int?)))
        :ret (s/or :ok ::model :err empty?))

(defn get-by-link [model]
  (log/debug "get by link" model)
  (let [link (str (:feed.item.logic/link model))
        data-model (dao/get-by-link {:feed.item/link link})]
    (if-not (nil? data-model)
      (dao-model->logic-model data-model))))

(s/fdef get-by-link
        :args (s/cat :model (s/keys :req [:feed.item.logic/link]))
        :ret (s/or :ok ::model :err nil?))

(defn get-by-links [models]
  (log/debug "get by" (count models) "links")
  (let [feeds-map (->> models
                       (group-by #(:feed.logic/id (:feed.item.logic/feed %)))
                       (map (fn [[k _]] [k (feed-logic/get-by-id {:feed.logic/id k})]))
                       (into {}))
        links (->> models
                   (map :feed.item.logic/link)
                   (map str)
                   (map #(assoc {} :feed.item/link %))
                   (reduce conj []))
        data-models (dao/get-by-links links)]
    (->> data-models
         (map #(dao-model->logic-model % (get feeds-map (:feed.item/feed_id %))))
         (reduce conj []))))

(s/fdef get-by-links
        :args (s/cat :model (s/coll-of (s/keys :req [:feed.item.logic/link])))
        :ret (s/or :ok (s/coll-of ::model) :err nil?))

;; create

(s/def ::create-model (s/keys :req [:feed.item.logic/feed
                                    :feed.item.logic/title
                                    :feed.item.logic/link
                                    :feed.item.logic/pub-time]
                              :opt [:feed.item.logic/id
                                    :feed.item.logic/version
                                    :feed.item.logic/order-id
                                    :feed.item.logic/insert-time
                                    :feed.item.logic/update-time
                                    :feed.item.logic/description]))

(defn logic-create-model->dao-model
  ([model] (logic-create-model->dao-model model (t/now)))
  ([model time]
   (let [{:feed.item.logic/keys [id version order-id insert-time update-time feed title link pub-time description]
          :or                   {id          (UUID/randomUUID)
                                 version     0
                                 order-id    (tc/to-long (:feed.item.logic/pub-time model))
                                 insert-time time}
          } model]
     {:feed.item/id          id
      :feed.item/version     version
      :feed.item/order_id    order-id
      :feed.item/insert_time insert-time
      :feed.item/update_time update-time
      :feed.item/feed_id     (:feed.logic/id feed)
      :feed.item/title       title
      :feed.item/link        (str link)
      :feed.item/pub_time    pub-time
      :feed.item/description description})))

(defn create [model]
  (log/info "create" model)
  (let [errors (specs/errors ::create-model model)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-logic-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed-item (get-by-link model)]
        feed-item
        (-> model
            (logic-create-model->dao-model)
            (dao/insert)
            (dao-model->logic-model))))))

(s/fdef create
        :args (s/cat :model ::create-model)
        :ret ::model)

(defn logic-create-models->dao-models [models]
  (let [now (t/now)]
    (->> models
         (map #(logic-create-model->dao-model % now))
         (reduce conj []))))

(defn create-multi [models]
  (log/info "create" (count models) "model(s)")
  (let [errors (specs/errors (s/coll-of ::create-model) models)]
    (if (not-empty errors)
      (do
        (log/info "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-logic-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [existing-models (get-by-links models)
            existing-links (->> existing-models
                                (map :feed.item.logic/link)
                                (reduce conj []))
            missing-models (->> models
                                (remove (fn [feed-item]
                                          (some #(= % (:feed.item.logic/link feed-item)) existing-links)
                                          ))
                                (reduce conj []))
            missing-models-feeds-map (->> missing-models
                                          (group-by #(:feed.logic/id (:feed.item.logic/feed %)))
                                          (map (fn [[k _]] [k (feed-logic/get-by-id {:feed.logic/id k})]))
                                          (into {}))]
        (concat
          existing-models
          (->> missing-models
               (logic-create-models->dao-models)
               (dao/insert-multi)
               (map #(dao-model->logic-model % (get missing-models-feeds-map (:feed.item/feed_id %))))))))))

(s/fdef create-multi
        :args (s/cat :models (s/coll-of ::create-model))
        :ret (s/coll-of ::model))

;; delete

(defn delete [model]
  (log/info "delete" model)
  (let [id (:feed.item.logic/id model)]
    (dao/delete {:feed.item/id id})))

(s/fdef delete
        :args (s/cat :model (s/keys :req [:feed.item.logic/id]))
        :ret (s/or :ok ::model :err nil?))