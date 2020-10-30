(ns rss-feed-reader.domain.account-feed-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.account-feed :refer :all]
            [rss-feed-reader.system_test :refer [with-system]]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.domain.account :as account]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [rss-feed-reader.db.provider.h2 :as datasource]
            [rss-feed-reader.db.migration :as migration])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))

(defn system-map []
  (component/system-map
    :datasource (datasource/map->H2Datasource {:config   {:h2-jdbc (str "jdbc:h2:mem:account-feed-test-"
                                                                        (gen/generate (s/gen string?)))}
                                               :on-start #(migration/migrate %)
                                               :on-stop  #(migration/rollback %)})
    :accounts (component/using
                (account/map->DbAccounts {})
                [:datasource])

    :feeds (component/using
             (feed/map->DbFeeds {})
             [:datasource])

    :accounts-feeds (component/using
                      (map->DbAccountsFeeds {})
                      [:datasource :accounts :feeds])
    ))

(deftest should-get
  (testing "should get"
    (with-system [system (system-map)]
      ; given
      (testing "by"
        (let [accounts (:accounts system)
              feeds (:feeds system)
              accounts-feeds (:accounts-feeds system)
              account (account/create! accounts (gen/generate (s/gen :rss-feed-reader.domain.account/model)))
              feed (feed/create! feeds (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
              model (-> (gen/generate (s/gen :rss-feed-reader.domain.account-feed/model))
                        (assoc :account.feed.domain/account account)
                        (assoc :account.feed.domain/feed feed)
                        ((partial create! accounts-feeds)))]
          (testing "id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-id accounts-feeds {:account.feed.domain/id (UUID/randomUUID)})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-id accounts-feeds model)]
                ; then
                (is (= model actual)))))
          (testing "account"
            (testing "and return models"
              ; when
              (let [actual (get-by-account accounts-feeds model)]
                ; then
                (is (= (seq [model]) actual))))
            (testing "and return empty"
              ; when
              (let [actual (get-by-account accounts-feeds {:account.feed.domain/account {:account.domain/id (UUID/randomUUID)}})]
                ; then
                (is (empty? actual)))))
          (testing "feed"
            (testing "and return models"
              ; when
              (let [actual (get-by-feed accounts-feeds model)]
                ; then
                (is (= (seq [model]) actual))))
            (let [actual (get-by-feed accounts-feeds {:account.feed.domain/feed {:feed.domain/id (UUID/randomUUID)}})]
              ; then
              (is (empty? actual))))
          (testing "account and feed"
            (testing "and return nil"
              ; when
              (let [actual (get-by-account-and-feed accounts-feeds {:account.feed.domain/account {:account.domain/id (UUID/randomUUID)}
                                                                    :account.feed.domain/feed    {:feed.domain/id (UUID/randomUUID)}})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-account-and-feed accounts-feeds model)]
                ; then
                (is (= model actual))))))))))

(deftest should-create
  (testing "should create"
    (with-system [system (system-map)]
      ; given
      (let [accounts (:accounts system)
            feeds (:feeds system)
            accounts-feeds (:accounts-feeds system)
            account (account/create! accounts (gen/generate (s/gen :rss-feed-reader.domain.account/model)))
            feed (feed/create! feeds (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
            create-model (-> (gen/generate (s/gen :rss-feed-reader.domain.account-feed/create-model))
                             (assoc :account.feed.domain/account account)
                             (assoc :account.feed.domain/feed feed))]
        (testing "throwing specs exception"
          ; when
          (try
            (create! accounts-feeds {})
            (do-report {:type     :fail,
                        :message  "uncaught exception",
                        :expected ExceptionInfo,
                        :actual   nil})
            (catch ExceptionInfo e
              ; then
              (let [data (ex-data e)
                    {:keys [cause reason details]} data]
                (is (= :account-feed-create cause))
                (is (= :invalid-spec reason))
                (is (not-empty details))))))
        (testing "and return existing model"
          ; when
          (let [expected (create! accounts-feeds create-model)
                actual (create! accounts-feeds create-model)]
            ; then
            (is (= expected actual))))
        (testing "and return new model"
          ; when
          (let [actual (create! accounts-feeds create-model)]
            ; then
            (is (= create-model (select-keys actual (keys create-model))))
            (is (= 0 (:account.feed.domain/version actual)))
            (is (not (nil? (:account.feed.domain/order-id actual))))
            (is (not (nil? (:account.feed.domain/insert-time actual))))
            (is (nil? (:account.feed.domain/update-time actual)))))))))

(deftest should-delete
  (testing "should delete"
    (with-system [system (system-map)]
      ; given
      (let [accounts-feeds (:accounts-feeds system)
            model (create! accounts-feeds (gen/generate (s/gen :rss-feed-reader.domain.account-feed/create-model)))]
        ; when
        (let [actual (delete! accounts-feeds model)]
          ; then
          (is (= 1 actual))
          (is (nil? (get-by-id accounts-feeds model))))))))