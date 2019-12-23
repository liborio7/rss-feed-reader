(ns rss-feed-reader.domain.feed_item
  (:require [rss-feed-reader.data.feed_item :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :feed.item.domain/id uuid?)
(s/def :feed.item.domain/version pos-int?)
(s/def :feed.item.domain/order-id pos-int?)
(s/def :feed.item.domain/insert-time inst?)
(s/def :feed.item.domain/update-time inst?)
(s/def :feed.item.domain/feed (s/keys :req [:feed.domain/id]))
(s/def :feed.item.domain/title string?)
(s/def :feed.item.domain/link string?)
(s/def :feed.item.domain/pub-time inst?)
(s/def :feed.item.domain/description string?)

(s/def :feed.item.domain/starting-after :feed.item.domain/order-id)
(s/def :feed.item.domain/limit pos-int?)

(s/def ::get-by-id-req (s/keys :req [:feed.item.domain/id]))

(s/def ::get-by-feed-req (s/keys :req [:feed.item.domain/feed]
                                 :opt [:feed.item.domain/starting-after
                                       :feed.item.domain/limit]))

(s/def ::delete-req (s/keys :req [:feed.item.domain/id]))

(s/def ::create-req (s/keys :req [:feed.item.domain/feed
                                  :feed.item.domain/title
                                  :feed.item.domain/link
                                  :feed.item.domain/pub-time]
                            :opt [:feed.item.domain/id
                                  :feed.item.domain/version
                                  :feed.item.domain/order-id
                                  :feed.item.domain/insert-time
                                  :feed.item.domain/update-time
                                  :feed.item.domain/description]))

(s/def ::resp (s/keys :req [:feed.item.domain/id
                            :feed.item.domain/feed
                            :feed.item.domain/title
                            :feed.item.domain/link
                            :feed.item.domain/pub-time
                            :feed.item.domain/description]))

;; conversion

(defn create-req->model [m]
  (let [now (tc/to-long (t/now))
        {:feed.item.domain/keys [id version order-id insert-time update-time feed title link pub-time description]
         :or                    {id          (java.util.UUID/randomUUID)
                                 version     0
                                 order-id    (:feed.item.domain/pub-time m)
                                 insert-time now}
         } m]
    {:feed.item/id          id
     :feed.item/version     version
     :feed.item/order_id    order-id
     :feed.item/insert_time insert-time
     :feed.item/update_time update-time
     :feed.item/feed_id     (:feed.domain/id feed)
     :feed.item/title       title
     :feed.item/link        link
     :feed.item/pub_time    pub-time
     :feed.item/description description}))

(defn model->response [m]
  (let [{:feed.item/keys [id feed_id title link pub-time description]} m
        feed (feed-mgr/get-by-id {:feed.domain/id feed_id})]
    {:feed.item.domain/id          id
     :feed.item.domain/feed        feed
     :feed.item.domain/title       title
     :feed.item.domain/link        link
     :feed.item.domain/pub-time    pub-time
     :feed.item.domain/description description}))

;; get

(defn get-by-id [req]
  (log/info "get by id" req)
  (let [id (:feed.item.domain/id req)
        model (dao/get-by-id {:feed.item/id id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret (s/or :ok ::resp :err nil?))

(defn get-by-feed [req]
  (log/info "get by feed" req)
  (let [{:feed.item.domain/keys [feed starting-after limit]
         :or                    {starting-after 0 limit 20}} req
        models (dao/get-by-feed-id {:feed.item/feed_id        (:feed.domain/id feed)
                                    :feed.item/starting-after starting-after
                                    :feed.item/limit          limit})]
    (map model->response models)))

(s/fdef get-by-feed
        :args (s/cat :req ::get-by-feed-req)
        :ret (s/or :ok ::resp :err empty?))

;; create

(defn create [req]
  (log/info "create" req)
  (let [errors (specs/errors ::create-req req)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-item-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (-> req
          (create-req->model)
          (dao/insert)
          (model->response)))))

(s/fdef create
        :args (s/cat :req ::create-req)
        :ret ::resp)

;; delete

(defn delete [req]
  (log/info "delete" req)
  (let [id (:feed.item.domain/id req)]
    (dao/delete {:feed.item/id id})))

(s/fdef delete
        :args (s/cat :req ::delete-req)
        :ret (s/or :ok ::resp :err nil?))