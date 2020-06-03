(ns rss-feed-reader.domain.job.logic-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.job.logic :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(deftest should-get
  (testing "should get"
    ; given
    (testing "by"
      (let [model (gen/generate (s/gen :rss-feed-reader.domain.job.logic/model))]
        (testing "id"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.domain.job.dao/get-by-id (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.job.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.domain.job.logic/model))]
              (with-redefs [rss-feed-reader.domain.job.dao/get-by-id (fn [_] dao-model)
                            rss-feed-reader.domain.job.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (is (= actual expected)))))))
        (testing "name"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.domain.job.dao/get-by-name (fn [_] nil)]
              ; then
              (let [actual (get-by-name model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.job.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.domain.job.logic/model))]
              (with-redefs [rss-feed-reader.domain.job.dao/get-by-name (fn [_] dao-model)
                            rss-feed-reader.domain.job.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-name model)]
                  (is (= actual expected)))))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.domain.job.logic/create-model))]
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
                  (is (= :job-logic-create cause))
                  (is (= :invalid-spec reason))
                  (is (= specs-errors details))))))))
      (testing "and return existing model"
        ; when
        (let [expected (gen/generate (s/gen :rss-feed-reader.domain.job.logic/model))]
          (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                        rss-feed-reader.domain.job.logic/get-by-name (fn [_] expected)]
            ; then
            (let [actual (create create-model)]
              (is (= actual expected))))))
      (testing "and return new model"
        ; when
        (let [dao-model (gen/generate (s/gen :rss-feed-reader.domain.job.dao/model))
              expected (gen/generate (s/gen :rss-feed-reader.domain.job.logic/model))]
          (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                        rss-feed-reader.domain.job.logic/get-by-name (fn [_] nil)
                        rss-feed-reader.domain.job.logic/logic-create-model->dao-model (fn [_] dao-model)
                        rss-feed-reader.domain.job.dao/insert (fn [_] dao-model)
                        rss-feed-reader.domain.job.logic/dao-model->logic-model (fn [_] expected)]
            ; then
            (let [actual (create create-model)]
              (is (= actual expected)))))))))