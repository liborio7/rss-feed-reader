(ns rss-feed-reader.core.account.logic-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.core.account.logic :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(deftest should-get
  (testing "should get"
    ; given
    (testing "by"
      (let [model (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
        (testing "id"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.account.dao/get-by-id (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (nil? actual))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
              (with-redefs [rss-feed-reader.core.account.dao/get-by-id (fn [_] dao-model)
                            rss-feed-reader.core.account.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (= actual expected))))))
        (testing "username"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.account.dao/get-by-username (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (nil? actual))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
              (with-redefs [rss-feed-reader.core.account.dao/get-by-username (fn [_] dao-model)
                            rss-feed-reader.core.account.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (= actual expected))))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.core.account.logic/create-model))]
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
                  (is (= :account-logic-create cause))
                  (is (= :invalid-spec reason))
                  (is (= specs-errors details)))))))
        (testing "and return existing model"
          ; when
          (let [expected (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.account.dao/get-by-username (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (= actual expected)))))
        (testing "and return new model"
          ; when
          (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.dao/model))
                expected (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.account.dao/get-by-username (fn [_] nil)
                          rss-feed-reader.core.account.logic/logic-create-model->dao-model (fn [_] dao-model)
                          rss-feed-reader.core.account.dao/insert (fn [_] dao-model)
                          rss-feed-reader.core.account.logic/dao-model->logic-model (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (= actual expected)))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [model (gen/generate (s/gen :rss-feed-reader.core.account.logic/model))]
      ; when
      (let [expected (gen/generate (s/gen int?))]
        (with-redefs [rss-feed-reader.core.account.dao/delete (fn [_] expected)]
          ; then
          (let [actual (delete model)]
            (is (= actual expected))))))))