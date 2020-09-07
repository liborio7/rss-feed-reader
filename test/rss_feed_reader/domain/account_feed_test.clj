(ns rss-feed-reader.domain.account-feed-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.account-feed :refer :all]
            [rss-feed-reader.domain.account :as accounts]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.db.datasource-test :refer [db-fixture]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once db-fixture)

(deftest should-get
  (testing "should get"
    ; given
    (testing "by"
      (let [account (accounts/create! (gen/generate (s/gen :rss-feed-reader.domain.account/model)))
            feed (feeds/create! (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
            model (-> (gen/generate (s/gen :rss-feed-reader.domain.account-feed/model))
                      (assoc :account.feed.domain/account account)
                      (assoc :account.feed.domain/feed feed)
                      (create!))]
        (testing "id"
          (testing "and return nil"
            ; when
            (let [actual (get-by-id {:account.feed.domain/id (java.util.UUID/randomUUID)})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-id model)]
              ; then
              (is (= model actual)))))
        (testing "account"
          (testing "and return models"
            ; when
            (let [actual (get-by-account model)]
              ; then
              (is (= (seq [model]) actual))))
          (testing "and return empty"
            ; when
            (let [actual (get-by-account {:account.feed.domain/account {:account.domain/id (java.util.UUID/randomUUID)}})]
              ; then
              (is (empty? actual)))))
        (testing "feed"
          (testing "and return models"
            ; when
            (let [actual (get-by-feed model)]
              ; then
              (is (= (seq [model]) actual))))
          (let [actual (get-by-feed {:account.feed.domain/feed {:feed.domain/id (java.util.UUID/randomUUID)}})]
            ; then
            (is (empty? actual))))
        (testing "account and feed"
          (testing "and return nil"
            ; when
            (let [actual (get-by-account-and-feed {:account.feed.domain/account {:account.domain/id (java.util.UUID/randomUUID)}
                                                   :account.feed.domain/feed    {:feed.domain/id (java.util.UUID/randomUUID)}})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-account-and-feed model)]
              ; then
              (is (= model actual)))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [account (accounts/create! (gen/generate (s/gen :rss-feed-reader.domain.account/model)))
          feed (feeds/create! (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
          create-model (-> (gen/generate (s/gen :rss-feed-reader.domain.account-feed/create-model))
                           (assoc :account.feed.domain/account account)
                           (assoc :account.feed.domain/feed feed))]
      (testing "throwing specs exception"
        ; when
        (try
          (create! {})
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
        (let [expected (create! create-model)
              actual (create! create-model)]
          ; then
          (is (= expected actual))))
      (testing "and return new model"
        ; when
        (let [actual (create! create-model)]
          ; then
          (is (= create-model (select-keys actual (keys create-model))))
          (is (= 0 (:account.feed.domain/version actual)))
          (is (not (nil? (:account.feed.domain/order-id actual))))
          (is (not (nil? (:account.feed.domain/insert-time actual))))
          (is (nil? (:account.feed.domain/update-time actual))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [model (create! (gen/generate (s/gen :rss-feed-reader.domain.account-feed/create-model)))]
      ; when
      (let [actual (delete! model)]
        (is (= 1 actual))
        (is (nil? (get-by-id model)))))))