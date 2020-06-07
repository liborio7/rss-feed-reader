(ns rss-feed-reader.domain.feed.logic-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [rss-feed-reader.domain.feed.logic :refer :all]
            [rss-feed-reader.domain.feed.dao :as dao])
  (:import (clojure.lang ExceptionInfo)))

(deftest should-get
  (testing "should get"
    (testing "all"
      (testing "with provided arguments"
        ; given
        (let [starting-after (gen/generate (s/gen nat-int?))]
          ; when
          (let [dao-models (gen/sample (s/gen :rss-feed-reader.domain.feed.dao/model))
                logic-model (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))
                expected (repeat (count dao-models) logic-model)]
            (with-redefs [rss-feed-reader.domain.feed.dao/get-all
                          (let [times (atom 0)]
                            (fn [_ sa]
                              (if (pos? @times)
                                dao-models
                                (do
                                  (swap! times inc)
                                  (cond
                                    (not= sa starting-after) (do-report {:type     :fail,
                                                                         :message  "wrong starting after",
                                                                         :expected starting-after,
                                                                         :actual   sa})
                                    :else dao-models)))))
                          rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] logic-model)]
              ; then
              (let [actual (take (count expected) (get-all :starting-after starting-after))]
                (is (= actual expected)))))))
      (testing "with default arguments"
        ; given
        (let [dao-models (gen/sample (s/gen :rss-feed-reader.domain.feed.dao/model))
              logic-model (gen/sample (s/gen :rss-feed-reader.domain.feed.logic/model))
              expected (repeat (count dao-models) logic-model)]
          (with-redefs [rss-feed-reader.domain.feed.dao/get-all
                        (let [default-starting-after 0
                              times (atom 0)]
                          (fn [_ sa]
                            (if (pos? @times)
                              dao-models
                              (do
                                (swap! times inc)
                                (cond
                                  (not= sa default-starting-after) (do-report {:type     :fail,
                                                                               :message  "wrong starting after",
                                                                               :expected default-starting-after,
                                                                               :actual   sa})
                                  :else dao-models)))))
                        rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] logic-model)]
            ; then
            (let [actual (take (count expected) (get-all))]
              (is (= actual expected)))))))
    (testing "by"
      ; given
      (let [model (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))
            models (gen/sample (s/gen :rss-feed-reader.domain.feed.logic/model))]
        (testing "id"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.domain.feed.dao/get-by-id (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.feed.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))]
              (with-redefs [rss-feed-reader.domain.feed.dao/get-by-id (fn [_] dao-model)
                            rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (is (= actual expected)))))))
        (testing "ids"
          (testing "and return models"
            ; when
            (let [dao-models (gen/sample (s/gen :rss-feed-reader.domain.feed.dao/model))
                  logic-model (gen/sample (s/gen :rss-feed-reader.domain.feed.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.domain.feed.dao/get-by-ids (fn [_] dao-models)
                            rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] logic-model)]
                ; then
                (let [actual (get-by-ids models)]
                  (is (= actual expected)))))))
        (testing "link"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.domain.feed.dao/get-by-link (fn [_] nil)]
              ; then
              (let [actual (get-by-link model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.feed.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))]
              (with-redefs [rss-feed-reader.domain.feed.dao/get-by-link (fn [_] dao-model)
                            rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-link model)]
                  (is (= actual expected)))))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/create-model))]
      (testing "throwing specs exception"
        ; when
        (let [specs-errors (gen/generate (s/gen map?))]
          (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] specs-errors)]
            ; then
            (try
              (create create-model)
              (do-report {:type     :fail,
                          :message  "uncaught exception",
                          :expected ExceptionInfo,
                          :actual   nil})
              (catch ExceptionInfo e
                (let [data (ex-data e)
                      {:keys [cause reason details]} data]
                  (is (= :feed-logic-create cause))
                  (is (= :invalid-spec reason))
                  (is (= specs-errors details))))))))
      (testing "and return existing model"
        ; when
        (let [expected (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))]
          (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                        rss-feed-reader.domain.feed.logic/get-by-link (fn [_] expected)]
            ; then
            (let [actual (create create-model)]
              (is (= actual expected))))))
      (testing "and return new model"
        ; when
        (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.feed.dao/model))
              expected (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))]
          (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                        rss-feed-reader.domain.feed.logic/get-by-link (fn [_] nil)
                        rss-feed-reader.domain.feed.logic/logic-create-model->dao-model (fn [_] dao-model)
                        rss-feed-reader.domain.feed.dao/insert (fn [_] dao-model)
                        rss-feed-reader.domain.feed.logic/dao-model->logic-model (fn [_] expected)]
            ; then
            (let [actual (create create-model)]
              (is (= actual expected)))))))))

(fact "2 + 2 should be equal to 4"
      (+ 2 2) => 5)

(fact "should delete"
      (let [model (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))
            expected (gen/generate (s/gen int?))]
        (delete model) => expected
        (provided (dao/delete model) => expected)))


;(deftest should-delete
;  (testing "should delete"
;    ; given
;    (let [model (gen/generate (s/gen :rss-feed-reader.domain.feed.logic/model))]
;      ; when
;      (let [expected (gen/generate (s/gen int?))]
;        (with-redefs [rss-feed-reader.domain.feed.dao/delete (fn [_] expected)]
;          ; then
;          (let [actual (delete model)]
;            (is (= actual expected))))))))