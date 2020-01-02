(ns rss-feed-reader.domain.feed
  (:require [rss-feed-reader.data.feed :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :feed.domain/id uuid?)
(s/def :feed.domain/version pos-int?)
(s/def :feed.domain/order-id pos-int?)
(s/def :feed.domain/insert-time inst?)
(s/def :feed.domain/update-time inst?)
(s/def :feed.domain/link uri?)

(s/def :feed.domain/starting-after :feed.domain/order-id)
(s/def :feed.domain/limit pos-int?)

(s/def ::get-by-id-req (s/keys :req [:feed.domain/id]))

(s/def ::get-by-link-req (s/keys :req [:feed.domain/link]))

(s/def ::get-all (s/keys :opt [:feed.domain/starting-after
                               :feed.domain/limit]))

(s/def ::create-req (s/keys :req [:feed.domain/link]
                            :opt [:feed.domain/id
                                  :feed.domain/version
                                  :feed.domain/order-id
                                  :feed.domain/insert-time
                                  :feed.domain/update-time]))

(s/def ::delete-req (s/keys :req [:feed.domain/id]))

(s/def ::resp (s/keys :req [:feed.domain/id
                            :feed.domain/link
                            :feed.domain/order-id]))

;; conversion

(defn create-req->model [m]
  (let [now (t/now)
        {:feed.domain/keys [id version order-id insert-time update-time link]
         :or               {id          (java.util.UUID/randomUUID)
                            version     0
                            order-id    (tc/to-long now)
                            insert-time now
                            }
         } m]
    {:feed/id          id
     :feed/version     version
     :feed/order_id    order-id
     :feed/insert_time (tc/to-long insert-time)
     :feed/update_time (tc/to-long update-time)
     :feed/link        (str link)}))

(defn model->response [m]
  (let [{:feed/keys [id order_id link]} m]
    {:feed.domain/id       id
     :feed.domain/order-id order_id
     :feed.domain/link     (uris/from-string link)}))

;; get

(defn get-by-id [req]
  (log/info "get by id" req)
  (let [id (:feed.domain/id req)
        model (dao/get-by-id {:feed/id id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret (s/or :ok ::resp :err nil?))

(defn get-by-link [req]
  (log/info "get by link" req)
  (let [link (str (:feed.domain/link req))
        model (dao/get-by-link {:feed/link link})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-link
        :args (s/cat :req ::get-by-link-req)
        :ret (s/or :ok ::resp :err nil?))

(defn get-all [req]
  (log/info "get all" req)
  (let [{:feed.domain/keys [starting-after limit]
         :or               {starting-after 0 limit 20}} req
        models (dao/get-all {:feed/starting-after starting-after
                             :feed/limit          limit})]
    (map model->response models)))

(s/fdef get-all
        :args (s/cat :req ::get-all)
        :ret (s/or :ok (s/coll-of ::resp) :err nil?))

;; create

(defn create [req]
  (log/info "create" req)
  (let [errors (specs/errors ::create-req req)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (if-let [feed (get-by-link req)]
        feed
        (-> req
            (create-req->model)
            (dao/insert)
            (model->response))))))

(s/fdef create
        :args (s/cat :req ::create-req)
        :ret ::resp)

;; delete

(defn delete [req]
  (log/info "delete" req)
  (let [id (:feed.domain/id req)]
    (dao/delete {:feed/id id})))

(s/fdef delete
        :args (s/cat :req ::delete-req)
        :ret int?)