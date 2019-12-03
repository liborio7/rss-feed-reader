(ns rss-feed-reader.feed.manager
  (:require [rss-feed-reader.feed.dao :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :feed.manager/id uuid?)
(s/def :feed.manager/version int?)
(s/def :feed.manager/insert-time inst?)
(s/def :feed.manager/update-time inst?)
(s/def :feed.manager/link uri?)


(s/def ::get-by-id-req (s/keys :req [:feed.manager/id]))

(s/def ::get-by-link-req (s/keys :req [:feed.manager/link]))

(s/def ::create-req (s/keys :req [:feed.manager/link]
                            :opt [:feed.manager/id
                                  :feed.manager/version
                                  :feed.manager/insert-time
                                  :feed.manager/update-time]))

(s/def ::delete-req (s/keys :req [:feed.manager/id]))

(s/def ::resp (s/keys :req [:feed.manager/id
                            :feed.manager/link]))

;; conversion

(defn create-req->model [m]
  (let [{:feed.manager/keys [id version insert-time update-time link]
         :or              {id          (java.util.UUID/randomUUID)
                           version     0
                           insert-time (tc/to-long (t/now))}
         } m]
    {:feed/id          id
     :feed/version     version
     :feed/insert_time insert-time
     :feed/update_time update-time
     :feed/link        (str link)}))

(defn model->response [m]
  (let [{:feed/keys [id link]} m]
    {:feed.manager/id   id
     :feed.manager/link (uris/from-string link)}))

;; get

(defn get-by-id [req]
  (log/info "get by id" req)
  (let [id (:feed.manager/id req)
        model (dao/get-by-id {:feed/id id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret (s/or :ok ::rest :err nil?))

(defn get-by-link [req]
  (log/info "get by link" req)
  (let [link (str (:feed.manager/link req))
        model (dao/get-by-link {:feed/link link})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-link
        :args (s/cat :req ::get-by-link-req)
        :ret (s/or :ok ::rest :err nil?))

;; create

(defn create [req]
  (log/info "create" req)
  (let [errors (specs/errors ::create-req req)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :feed-manager-create
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
  (let [id (:feed.manager/id req)]
    (dao/delete {:feed/id id})))

(s/fdef create
        :args (s/cat :req ::delete-req)
        :ret int?)