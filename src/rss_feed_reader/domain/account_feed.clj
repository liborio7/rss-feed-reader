(ns rss-feed-reader.domain.account-feed
  (:require [rss-feed-reader.utils.spec :as specs]
            [rss-feed-reader.db.client :as db]
            [clojure.spec.alpha :as spec]
            [rss-feed-reader.utils.time :as time]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.account :as account]
            [rss-feed-reader.domain.feed :as feed])
  (:import (java.util UUID)))

(def table :account-feed)

;; model

(spec/def :account.feed.domain/id uuid?)
(spec/def :account.feed.domain/version nat-int?)
(spec/def :account.feed.domain/order-id nat-int?)
(spec/def :account.feed.domain/insert-time ::time/time)
(spec/def :account.feed.domain/update-time (spec/nilable ::time/time))
(spec/def :account.feed.domain/account :rss-feed-reader.domain.account/model)
(spec/def :account.feed.domain/feed :rss-feed-reader.domain.feed/model)

(spec/def ::model (spec/keys :req [:account.feed.domain/id
                                   :account.feed.domain/version
                                   :account.feed.domain/order-id
                                   :account.feed.domain/account
                                   :account.feed.domain/feed]))

(spec/def ::create-model (spec/keys :req [:account.feed.domain/account
                                          :account.feed.domain/feed]
                                    :opt [:account.feed.domain/id]))

;; conversion

(defn model->db [model]
  (let [now (time/instant-now)
        {:account.feed.domain/keys [id account feed]
         :or                       {id (UUID/randomUUID)}} model]
    {:account.feed/id          id
     :account.feed/version     0
     :account.feed/order-id    (time/instant->long now)
     :account.feed/insert-time now
     :account.feed/update-time nil
     :account.feed/account-id  (:account.domain/id account)
     :account.feed/feed-id     (:feed.domain/id feed)}))

(defn db->model [model account feed]
  (let [{:account.feed/keys [id version order-id insert-time update-time]} model]
    {:account.feed.domain/id          id
     :account.feed.domain/version     version
     :account.feed.domain/order-id    order-id
     :account.feed.domain/insert-time insert-time
     :account.feed.domain/update-time update-time
     :account.feed.domain/account     account
     :account.feed.domain/feed        feed}))

;; component

(defprotocol AccountsFeeds
  (get-by-id [this model])
  (get-by-account [this model] [this model opts])
  (get-by-feed [this model] [this model opts])
  (get-by-account-and-feed [this model])
  (create! [this model])
  (delete! [this model]))


(defrecord DbAccountsFeeds [datasource accounts feeds]
  AccountsFeeds

  ;; get

  (get-by-id [_ model]
    (log/debug "get by id" model)
    (db/with-transaction [conn datasource]
      (let [id (:account.feed.domain/id model)
            db-model (db/select conn table {:where [:= :account.feed/id id]})
            db->model* (fn [db-model]
                         (let [{:account.feed/keys [account-id feed-id]} db-model
                               account (account/get-by-id accounts {:account.domain/id account-id})
                               feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                           (db->model db-model account feed)))]
        (when db-model
          (db->model* db-model)))))

  (get-by-account [this model]
    (get-by-account this model {:starting-after 0 :limit 20}))

  (get-by-account [_ model {:keys [starting-after limit]}]
    (log/debug "get by account" model "starting after" starting-after "limit" limit)
    (let [account (:account.feed.domain/account model)
          account-id (:account.domain/id account)
          select #(db/with-transaction [conn datasource]
                    (db/select-values conn table {:where    [:and
                                                             [:= :account.feed/account-id account-id]
                                                             [:> :account.feed/order-id %]]
                                                  :order-by [[:account.feed/order-id :asc]]
                                                  :limit    20}))
          db-models (db/paginate-select select :account.feed/order-id starting-after)
          feed-by-feed-id (->> db-models
                               (map :account.feed/feed-id)
                               (map (partial hash-map :feed.domain/id))
                               (feed/get-by-ids feeds)
                               (mapcat (juxt :feed.domain/id identity))
                               (apply hash-map))
          db->model* (fn [db-model]
                       (let [feed (get feed-by-feed-id (:account.feed/feed-id db-model))]
                         (db->model db-model account feed)))]
      (map db->model* db-models)))

  (get-by-feed [this model]
    (get-by-feed this model {:starting-after 0 :limit 20}))

  (get-by-feed [_ model {:keys [starting-after limit]}]
    (log/debug "get by feed" model "starting after" starting-after "limit" limit)
    (let [feed (:account.feed.domain/feed model)
          feed-id (:feed.domain/id feed)
          select #(db/with-transaction [conn datasource]
                    (db/select-values conn table {:where    [:and
                                                             [:= :account.feed/feed-id feed-id]
                                                             [:> :account.feed/order-id %]]
                                                  :order-by [[:account.feed/order-id :asc]]
                                                  :limit    20}))
          db-models (db/paginate-select select :account.feed/order-id starting-after)
          account-by-account-id (->> db-models
                                     (map :account.feed/account-id)
                                     (map (partial hash-map :account.domain/id))
                                     (account/get-by-ids accounts)
                                     (mapcat (juxt :account.domain/id identity))
                                     (apply hash-map))
          db->model* (fn [db-model]
                       (let [account (get account-by-account-id (:account.feed/account-id db-model))]
                         (db->model db-model account feed)))]
      (map db->model* db-models)))

  (get-by-account-and-feed [_ model]
    (log/debug "get by account and feed" model)
    (db/with-transaction [conn datasource]
      (let [account-id (:account.domain/id (:account.feed.domain/account model))
            feed-id (:feed.domain/id (:account.feed.domain/feed model))
            db-model (db/select conn table {:where [:and
                                                    [:= :account.feed/account-id account-id]
                                                    [:= :account.feed/feed-id feed-id]]})
            db->model* (fn [db-model]
                         (let [{:account.feed/keys [account-id feed-id]} db-model
                               account (account/get-by-id accounts {:account.domain/id account-id})
                               feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                           (db->model db-model account feed)))]
        (when db-model
          (db->model* db-model)))))

  ;; create

  (create! [this model]
    (log/info "create" model)
    (let [errors (specs/errors ::create-model model)]
      (if (not-empty errors)
        (do
          (log/info "invalid request" errors)
          (throw (ex-info "invalid request"
                          {:cause   :account-feed-create
                           :reason  :invalid-spec
                           :details errors})))
        (let [account (:account.feed.domain/account model)
              feed (:account.feed.domain/feed model)
              account-feed (get-by-account-and-feed this {:account.feed.domain/account account
                                                          :account.feed.domain/feed    feed})]
          (if account-feed
            account-feed
            (let [db->model* (fn [db-model]
                               (let [{:account.feed/keys [account-id feed-id]} db-model
                                     account (account/get-by-id accounts {:account.domain/id account-id})
                                     feed (feed/get-by-id feeds {:feed.domain/id feed-id})]
                                 (db->model db-model account feed)))]
              (db/with-transaction [tx datasource]
                (->> model
                     (model->db)
                     (db/insert! tx table)
                     (db->model*)))))))))

  ;; delete

  (delete! [_ model]
    (log/info "delete" model)
    (db/with-transaction [tx datasource]
      (let [id (:account.feed.domain/id model)]
        (db/delete! tx table {:where [:= :account.feed/id id]})))))