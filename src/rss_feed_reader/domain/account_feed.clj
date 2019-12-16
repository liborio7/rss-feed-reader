(ns rss-feed-reader.domain.account_feed
  (:require [rss-feed-reader.data.account_feed :as dao]
            [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.account :as account-mgr]
            [clojure.spec.alpha :as s]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [clojure.tools.logging :as log]))

;; spec

(s/def :account.feed.domain/id uuid?)
(s/def :account.feed.domain/version pos-int?)
(s/def :account.feed.domain/order-id pos-int?)
(s/def :account.feed.domain/insert-time inst?)
(s/def :account.feed.domain/update-time inst?)
(s/def :account.feed.domain/account-id :account.domain/id)
(s/def :account.feed.domain/feed-id :feed.domain/id)
(s/def :account.feed.domain/account (s/keys :req [:account.domain/id]))
(s/def :account.feed.domain/feed (s/keys :req [:feed.domain/id]))

(s/def :account.feed.domain/starting-after :feed.domain/order-id)
(s/def :account.feed.domain/limit pos-int?)

(s/def ::get-by-id-req (s/keys :req [:account.feed.domain/id]))

(s/def ::get-by-account-id-req (s/keys :req [:account.feed.domain/account-id]
                                       :opt [:account.feed.domain/starting-after
                                             :account.feed.domain/limit]))

(s/def ::get-by-account-id-and-feed-id-req (s/keys :req [:account.feed.domain/account-id
                                                         :account.feed.domain/feed-id]))

(s/def ::delete-req (s/keys :req [:account.feed.domain/id]))

(s/def ::create-req (s/keys :req [:account.feed.domain/account
                                  :account.feed.domain/feed]
                            :opt [:account.feed.domain/id
                                  :account.feed.domain/version
                                  :account.feed.domain/order-id
                                  :account.feed.domain/insert-time
                                  :account.feed.domain/update-time]))

(s/def ::resp (s/keys :req [:account.feed.domain/id
                            :account.feed.domain/account
                            :account.feed.domain/feed]))

;; conversion

(defn create-req->model [m]
  (let [now (tc/to-long (t/now))
        {:account.feed.domain/keys [id version order-id insert-time update-time account feed]
         :or                       {id          (java.util.UUID/randomUUID)
                                    version     0
                                    order-id    now
                                    insert-time now}
         } m]
    {:account.feed/id          id
     :account.feed/version     version
     :account.feed/order_id    order-id
     :account.feed/insert_time insert-time
     :account.feed/update_time update-time
     :account.feed/account_id  (:account.domain/id account)
     :account.feed/feed_id     (:feed.domain/id feed)}))

(defn model->response [m]
  (let [{:account.feed/keys [id account_id feed_id]} m
        account (account-mgr/get-by-id {:account.domain/id account_id})
        feed (feed-mgr/get-by-id {:feed.domain/id feed_id})]
    {:account.feed.domain/id      id
     :account.feed.domain/account account
     :account.feed.domain/feed    feed}))

;; get

(defn get-by-id [req]
  (log/info "get by id" req)
  (let [id (:account.feed.domain/id req)
        model (dao/get-by-id {:account.feed/id id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-id
        :args (s/cat :req ::get-by-id-req)
        :ret (s/or :ok ::resp :err nil?))

(defn get-by-account-id [req]
  (log/info "get by account id" req)
  (let [{:account.feed.domain/keys [account-id starting-after limit]
         :or                       {starting-after 0 limit 20}} req
        models (dao/get-by-account-id {:account.feed/account_id     account-id
                                       :account.feed/starting-after starting-after
                                       :account.feed/limit          limit})]
    (map model->response models)))

(s/fdef get-by-account-id
        :args (s/cat :req ::get-by-account-id-req)
        :ret (s/or :ok ::resp :err empty?))

(defn get-by-account-id-and-feed-id [req]
  (log/info "get by account id and feed id" req)
  (let [account-id (:account.feed.domain/account-id req)
        feed-id (:account.feed.domain/feed-id req)
        model (dao/get-by-account-id-and-feed-id {:account.feed/account_id account-id
                                                  :account.feed/feed_id    feed-id})]
    (if-not (nil? model)
      (model->response model))))

(s/fdef get-by-account-id-and-feed-id
        :args (s/cat :req ::get-by-account-id-and-feed-id-req)
        :ret (s/or :ok ::resp :err nil?))

;; create

(defn create [req]
  (log/info "create" req)
  (let [errors (specs/errors ::create-req req)]
    (if (not-empty errors)
      (do
        (log/warn "invalid request" errors)
        (throw (ex-info "invalid request"
                        {:cause   :account-feed-domain-create
                         :reason  :invalid-spec
                         :details errors})))
      (let [account-id (:account.domain/id (:account.feed.domain/account req))
            feed-id (:feed.domain/id (:account.feed.domain/feed req))
            account-feed (get-by-account-id-and-feed-id {:account.feed.domain/account-id account-id
                                                         :account.feed.domain/feed-id    feed-id})]
        (if-not (nil? account-feed)
          account-feed
          (-> req
              (create-req->model)
              (dao/insert)
              (model->response)))))))

(s/fdef create
        :args (s/cat :req ::create-req)
        :ret ::resp)

;; delete

(defn delete [req]
  (log/info "delete" req)
  (let [id (:account.feed.domain/id req)]
    (dao/delete {:account.feed/id id})))

(s/fdef delete
        :args (s/cat :req ::delete-req)
        :ret (s/or :ok ::resp :err nil?))