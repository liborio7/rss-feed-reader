(ns rss-feed-reader.domain.feed-item
  (:require [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [clojure.spec.alpha :as spec]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as time]
            [rss-feed-reader.utils.uri :as uris])
  (:import (java.util UUID)))

(def table :feed-item)

;; model

(spec/def :feed.item.domain/id uuid?)
(spec/def :feed.item.domain/version nat-int?)
(spec/def :feed.item.domain/order-id nat-int?)
(spec/def :feed.item.domain/insert-time ::time/time)
(spec/def :feed.item.domain/update-time (spec/nilable ::time/time))
(spec/def :feed.item.domain/feed :rss-feed-reader.domain.feed/model)
(spec/def :feed.item.domain/title string?)
(spec/def :feed.item.domain/link uri?)
(spec/def :feed.item.domain/pub-time ::time/time)
(spec/def :feed.item.domain/description (spec/nilable string?))

(spec/def ::model (spec/keys :req [:feed.item.domain/id
                                   :feed.item.domain/version
                                   :feed.item.domain/order-id
                                   :feed.item.domain/insert-time
                                   :feed.item.domain/update-time
                                   :feed.item.domain/feed
                                   :feed.item.domain/title
                                   :feed.item.domain/link
                                   :feed.item.domain/pub-time
                                   :feed.item.domain/description]))

(spec/def ::create-model (spec/keys :req [:feed.item.domain/feed
                                          :feed.item.domain/title
                                          :feed.item.domain/link
                                          :feed.item.domain/pub-time]
                                    :opt [:feed.item.domain/id
                                          :feed.item.domain/description]))
;; conversion

(defn model->db [model]
  (let [now (time/instant-now)
        {:feed.item.domain/keys [id feed title link pub-time description]
         :or                    {id       (UUID/randomUUID)
                                 pub-time now}} model]
    {:feed.item/id          id
     :feed.item/version     0
     :feed.item/order-id    (time/instant->long pub-time)
     :feed.item/insert-time now
     :feed.item/update-time nil
     :feed.item/feed-id     (:feed.domain/id feed)
     :feed.item/title       title
     :feed.item/link        (str link)
     :feed.item/pub-time    pub-time
     :feed.item/description description}))

(defn db->model
  [model feed]
  (let [{:feed.item/keys [id version order-id insert-time update-time title link pub-time description]} model]
    {:feed.item.domain/id          id
     :feed.item.domain/version     version
     :feed.item.domain/order-id    order-id
     :feed.item.domain/insert-time insert-time
     :feed.item.domain/update-time update-time
     :feed.item.domain/feed        feed
     :feed.item.domain/title       title
     :feed.item.domain/link        (uris/from-string link)
     :feed.item.domain/pub-time    pub-time
     :feed.item.domain/description description}))

;; component

(defprotocol FeedsItems
  (get-all [this] [this opts])
  (get-by-id [this model])
  (get-by-feed [this model] [this model opts])
  (get-by-link [this model])
  (get-by-links [this models])
  (create! [this model])
  (create-multi! [this models])
  (delete! [this model])
  (delete-older-than! [this instant]))

(defrecord DbFeedsItems [datasource feeds]
  FeedsItems

  ;; get

  (get-all [this]
    (get-all this {:starting-after 0 :limit 0}))

  (get-all [_ {:keys [starting-after limit]}]
    (log/debug "get all starting-after" starting-after "limit" limit)
    (let [select #(db/with-transaction [conn datasource]
                    (db/select-values conn table {:where    [:> :feed.item/order-id %]
                                                  :order-by [[:feed.item/order-id :asc]]
                                                  :limit    20}))
          db-models (db/paginate-select select :feed.item/order-id starting-after)
          db->model* (fn [db-model]
                       (let [{:feed.item/keys [feed-id]} db-model
                             feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                         (db->model db-model feed)))]
      (map db->model* db-models)))

  (get-by-id [_ model]
    (log/debug "get by id" model)
    (db/with-transaction [conn datasource]
      (let [id (:feed.item.domain/id model)
            db-model (db/select conn table {:where [:= :feed.item/id id]})
            db->model* (fn [db-model]
                         (let [{:feed.item/keys [feed-id]} db-model
                               feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                           (db->model db-model feed)))]
        (when db-model
          (db->model* db-model)))))

  (get-by-feed [this model]
    (get-by-feed this model {:starting-after 0 :limit 20}))

  (get-by-feed [_ model {:keys [starting-after limit]}]
    (log/debug "get by feed" model "starting-after" starting-after "limit" limit)
    (let [feed (:feed.item.domain/feed model)
          feed-id (:feed.domain/id feed)
          select #(db/with-transaction [conn datasource]
                    (db/select-values conn table {:where    [:and
                                                             [:= :feed.item/feed-id feed-id]
                                                             [:> :feed.item/order-id %]]
                                                  :order-by [[:feed.item/order-id :asc]]
                                                  :limit    20}))
          db-models (db/paginate-select select :feed.item/order-id starting-after)
          db->model* (fn [db-model]
                       (db->model db-model feed))]
      (map db->model* db-models)))

  (get-by-link [_ model]
    (log/debug "get by link" model)
    (db/with-transaction [conn datasource]
      (let [link (str (:feed.item.domain/link model))
            db-model (db/select conn table {:where [:= :feed.item/link link]})
            db->model* (fn [db-model]
                         (let [{:feed.item/keys [feed-id]} db-model
                               feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                           (db->model db-model feed)))]
        (when db-model
          (db->model* db-model)))))

  (get-by-links [_ models]
    (log/debug "get by" (count models) "links")
    (db/with-transaction [conn datasource]
      (let [links (->> models
                       (map :feed.item.domain/link)
                       (map str))
            feed-by-feed-id (->> models
                                 (map :feed.item.domain/feed)
                                 (mapcat (juxt :feed.domain/id identity))
                                 (apply hash-map))
            db-models (when (not-empty links)
                        (db/select-values conn table {:where [:in :feed.item/link links]}))
            db->model* (fn [db-model]
                         (let [feed (get feed-by-feed-id (:feed.item/feed-id db-model))]
                           (db->model db-model feed)))]
        (map db->model* db-models))))

  ;; create

  (create! [this model]
    (log/info "create" model)
    (let [errors (specs/errors ::create-model model)]
      (if (not-empty errors)
        (do
          (log/info "invalid request" errors)
          (throw (ex-info "invalid request"
                          {:cause   :feed-item-create
                           :reason  :invalid-spec
                           :details errors})))
        (if-let [feed-item (get-by-link this model)]
          feed-item
          (let [db->model* (fn [db-model]
                             (let [feed (:feed.item.domain/feed model)]
                               (db->model db-model feed)))]
            (db/with-transaction [tx datasource]
              (->> model
                   (model->db)
                   (db/insert! tx table)
                   (db->model*))))))))

  (create-multi! [this models]
    (log/info "create" (count models) "model(s)")
    (let [errors (specs/errors (spec/coll-of ::create-model) models)]
      (if (not-empty errors)
        (do
          (log/info "invalid request" errors)
          (throw (ex-info "invalid request"
                          {:cause   :feed-item-create
                           :reason  :invalid-spec
                           :details errors})))
        (let [feed-by-feed-id (->> models
                                   (map :feed.item.domain/feed)
                                   (mapcat (juxt :feed.domain/id identity))
                                   (apply hash-map))
              existing-models (get-by-links this models)
              existing-links (map :feed.item.domain/link existing-models)
              missing-models (remove
                               (fn [model]
                                 (let [link (:feed.item.domain/link model)]
                                   (some (partial = link) existing-links)))
                               models)]
          (concat
            existing-models
            (let [db->model* (fn [db-model]
                               (let [feed (get feed-by-feed-id (:feed.item/feed-id db-model))]
                                 (db->model db-model feed)))]
              (when (not-empty missing-models)
                (db/with-transaction [tx datasource]
                  (->> missing-models
                       (map model->db)
                       (db/insert-values! tx table)
                       (mapv db->model*))))))))))

  ;; delete

  (delete! [_ model]
    (log/info "delete" model)
    (db/with-transaction [tx datasource]
      (let [id (:feed.item.domain/id model)]
        (db/delete! tx table {:where [:= :feed.item/id id]}))))

  (delete-older-than! [_ instant]
    (log/info "delete older than" instant)
    (db/with-transaction [tx datasource]
      (let [order-id (time/instant->long instant)]
        (db/delete! tx table {:where [:< :feed.item/order-id order-id]})))))