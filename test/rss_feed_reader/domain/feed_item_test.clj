(ns rss-feed-reader.domain.feed-item-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.feed-item :refer :all]
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
      (let [feed (feeds/create! (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
            model (-> (gen/generate (s/gen :rss-feed-reader.domain.feed-item/model))
                      (assoc :feed.item.domain/feed feed)
                      (create!))]
        (testing "id"
          (testing "and return nil"
            ; when
            (let [actual (get-by-id {:feed.item.domain/id (java.util.UUID/randomUUID)})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-id model)]
              ; then
              (is (= model actual)))))
        (testing "feed"
          (testing "and return models"
            ; when
            (let [actual (get-by-feed model)]
              ; then
              (is (= (seq [model]) actual))))
          (let [actual (get-by-feed {:feed.item.domain/feed {:feed.domain/id (java.util.UUID/randomUUID)}})]
            ; then
            (is (empty? actual))))
        (testing "link"
          (testing "and return nil"
            ; when
            (let [actual (get-by-link {:feed.item.domain/link (gen/generate (s/gen :feed.item.domain/link))})]
              ; then
              (is (nil? actual))))
          (testing "and return model"
            ; when
            (let [actual (get-by-link model)]
              ; then
              (is (= model actual)))))
        (testing "links"
          (testing "and return models"
            ; when
            (let [actual (get-by-links [model])]
              ; then
              (is (= (seq [model]) actual)))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [feed (feeds/create! (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
          create-model (-> (gen/generate (s/gen :rss-feed-reader.domain.feed-item/create-model))
                           (assoc :feed.item.domain/feed feed))]
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
              (is (= :feed-item-create cause))
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
          (is (= 0 (:feed.item.domain/version actual)))
          (is (not (nil? (:feed.item.domain/order-id actual))))
          (is (not (nil? (:feed.item.domain/insert-time actual))))
          (is (nil? (:feed.item.domain/update-time actual)))))
      (testing "multi"
        (testing "throwing specs exception"
          ; when
          (try
            (create-multi! [{}])
            (do-report {:type     :fail,
                        :message  "uncaught exception",
                        :expected ExceptionInfo,
                        :actual   nil})
            (catch ExceptionInfo e
              ; then
              (let [data (ex-data e)
                    {:keys [cause reason details]} data]
                (is (= :feed-item-create cause))
                (is (= :invalid-spec reason))
                (is (not-empty details))))))
        (testing "and return existing models"
          ; when
          (let [expected (create-multi! [create-model])
                actual (create-multi! [create-model])]
            ; then
            (is (= expected actual))))
        (testing "and return new models"
          ; when
          (let [actual (create-multi! [create-model])
                actual (first actual)]
            ; then
            (is (= 0 (:feed.item.domain/version actual)))
            (is (not (nil? (:feed.item.domain/order-id actual))))
            (is (not (nil? (:feed.item.domain/insert-time actual))))
            (is (nil? (:feed.item.domain/update-time actual)))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [model (create! (gen/generate (s/gen :rss-feed-reader.domain.feed-item/create-model)))]
      ; when
      (let [actual (delete! model)]
        (is (= 1 actual))
        (is (nil? (get-by-id model)))))))