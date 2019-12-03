(ns rss-feed-reader.feed.manager
  (:require [rss-feed-reader.feed.dao :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.utils.uri :as uris]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :feed.logic/id uuid?)
(s/def :feed.logic/version int?)
(s/def :feed.logic/insert-time inst?)
(s/def :feed.logic/update-time inst?)
(s/def :feed.logic/title (s/and string? #(< 0 (count %) 200)))
(s/def :feed.logic/link uri?)
(s/def :feed.logic/description (s/and string? #(< 0 (count %) 600)))

(s/def ::create-req (s/keys :req [:feed.logic/title
                                  :feed.logic/link]
                            :opt [:feed.logic/id
                                  :feed.logic/version
                                  :feed.logic/insert-time
                                  :feed.logic/update-time
                                  :feed.logic/description]))

(s/def ::get-by-id-req (s/keys :req [:feed.logic/id]))

(s/def ::resp (s/keys :req [:feed.logic/id
                            :feed.logic/title
                            :feed.logic/link]
                      :opt [:feed.logic/description]))

;; conversion

(defn create-req->model [req]
  (let [{:feed.logic/keys [id version insert-time update-time title link description]
         :or              {id          (java.util.UUID/randomUUID)
                           version     0
                           insert-time (tc/to-long (t/now))}
         } req]
    {:feed/id          id
     :feed/version     version
     :feed/insert_time insert-time
     :feed/update_time update-time
     :feed/title       title
     :feed/link        (str link)
     :feed/description description}))

(defn get-by-id-req->model [req]
  {:feed/id (:feed.logic/id req)})

(defn delete-req->model [req]
  {:feed/id (:feed.logic/id req)})

(defn model->response [model]
  (let [{:feed/keys [id title link description]} model]
    {:feed.logic/id          id
     :feed.logic/title       title
     :feed.logic/link        (uris/from-string link)
     :feed.logic/description description}))

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
      (-> req
          (create-req->model)
          (dao/insert)
          (model->response)))))

(s/fdef create
        :args (s/cat :req ::create-req)
        :ret ::resp)

;; get

(defn get-by-id [req]
  (log/info "get" req)
  (let [model (-> (get-by-id-req->model req)
                  (dao/get-by-id))]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret ::resp)

;; delete

(defn delete [req]
  (log/info "delete" req)
  (-> (delete-req->model req)
      (dao/delete)))