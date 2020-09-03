(ns rss-feed-reader.domain.account-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.account :refer :all]
            [rss-feed-reader.db.postgres-test :refer [db-fixture]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(use-fixtures :once db-fixture)

(deftest should-get
  (testing "should get"
    ; given
    (testing "by"
      (let [model (gen/generate (s/gen :rss-feed-reader.domain.account/model))
            model (create! model)]
        (testing "id"
          (testing "and return nil"
            ; when
            (let [actual (get-by-id {:account.domain/id (java.util.UUID/randomUUID)})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-id model)]
              ; then
              (is (= model actual)))))
        (testing "ids"
          (testing "and return models"
            ; when
            (let [actual (get-by-ids [model])]
              ; then
              (is (= [model] actual)))))
        (testing "chat id"
          (testing "and return nil"
            ; when
            (let [actual (get-by-chat-id {:account.domain/chat-id (inc (:account.domain/chat-id model))})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-chat-id model)]
              ; then
              (is (= model actual)))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.domain.account/create-model))]
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
              (is (= :account-create cause))
              (is (= :invalid-spec reason))
              (is (not-empty details))))))
      (testing "and return existing model"
        ; when
        (let [model (create! create-model)
              actual (create! create-model)]
          ; then
          (is (= model actual))))
      (testing "and return new model"
        ; when
        (let [actual (create! create-model)]
          ; then
          (is (= create-model (select-keys actual (keys create-model))))
          (is (= 0 (:account.domain/version actual)))
          (is (not (nil? (:account.domain/order-id actual))))
          (is (not (nil? (:account.domain/insert-time actual))))
          (is (nil? (:account.domain/update-time actual))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.domain.account/create-model))
          model (create! create-model)]
      ; when
      (let [actual (delete! model)]
        (is (= 1 actual))
        (is (nil? (get-by-id model)))))))