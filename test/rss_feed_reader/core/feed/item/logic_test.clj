(ns rss-feed-reader.core.feed.item.logic-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.core.feed.item.logic :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(deftest should-get
  (testing "should get"
    (testing "by"
      ; given
      (let [model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
            models (gen/sample (s/gen :rss-feed-reader.core.feed.item.logic/model))]
        (testing "id"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-id (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (nil? actual))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))]
              (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-id (fn [_] dao-model)
                            rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (is (= actual expected)))))))
        (testing "feed"
          (testing "with provided arguments"
            ; given
            (let [starting-after (gen/generate (s/gen nat-int?))
                  limit (gen/generate (s/gen nat-int?))]
              ; when
              (let [dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                    logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                    expected (repeat (count dao-models) logic-model)]
                (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-feed-id
                              (fn [_ _ sa _ l]
                                (cond
                                  (not= sa starting-after) (do-report {:type     :fail,
                                                                       :message  "wrong starting after",
                                                                       :expected starting-after,
                                                                       :actual   sa})
                                  (not= l limit) (do-report {:type     :fail,
                                                             :message  "wrong limit",
                                                             :expected limit,
                                                             :actual   l})
                                  :else dao-models))
                              rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_ _] logic-model)]
                  ; then
                  (let [actual (get-by-feed model :starting-after starting-after :limit limit)]
                    (is (= actual expected)))))))
          (testing "with default arguments"
            ; given
            (let [dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-feed-id
                            (fn [_ _ sa _ l]
                              (let [default-starting-after 0
                                    default-limit 20]
                                (cond
                                  (not= sa default-starting-after) (do-report {:type     :fail,
                                                                               :message  "wrong starting after",
                                                                               :expected default-starting-after,
                                                                               :actual   sa})
                                  (not= l default-limit) (do-report {:type     :fail,
                                                                     :message  "wrong limit",
                                                                     :expected default-limit,
                                                                     :actual   l})
                                  :else dao-models)))
                            rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_ _] logic-model)]
                ; then
                (let [actual (get-by-feed model)]
                  (is (= actual expected)))))))
        (testing "link"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-link (fn [_] nil)]
              ; then
              (let [actual (get-by-link model)]
                (nil? actual))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))]
              (with-redefs [rss-feed-reader.core.feed.item.dao/get-by-link (fn [_] dao-model)
                            rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-link model)]
                  (is (= actual expected)))))))
        (testing "links"
          (testing "and return models"
            ; when
            (let [feed (gen/generate (s/gen :rss-feed-reader.core.feed.logic/model))
                  dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.core.feed.logic/get-by-id (fn [_] feed)
                            rss-feed-reader.core.feed.item.dao/get-by-links (fn [_] dao-models)
                            rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_ _] logic-model)]
                ; then
                (let [actual (get-by-links models)]
                  (is (= actual expected)))))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/create-model))
          create-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.logic/create-model))]
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
                  (is (= :feed-item-logic-create cause))
                  (is (= :invalid-spec reason))
                  (is (= specs-errors details)))))))
        (testing "and return existing model"
          ; when
          (let [expected (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.feed.item.logic/get-by-link (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (is (= actual expected))))))
        (testing "and return new model"
          ; when
          (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.dao/model))
                expected (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.feed.item.logic/get-by-link (fn [_] nil)
                          rss-feed-reader.core.feed.item.logic/logic-create-model->dao-model (fn [_] dao-model)
                          rss-feed-reader.core.feed.item.dao/insert (fn [_] dao-model)
                          rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (is (= actual expected)))))))
      (testing "multi"
        (testing "throwing specs exception"
          ; when
          (let [specs-errors (gen/generate (s/gen map?))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] specs-errors)]
              ; then
              (try
                (create-multi create-models)
                (do-report {:type     :fail,
                            :message  "uncaught exception",
                            :expected ExceptionInfo,
                            :actual   nil})
                (catch ExceptionInfo e
                  (let [data (ex-data e)
                        {:keys [cause reason details]} data]
                    (is (= :feed-item-logic-create cause))
                    (is (= :invalid-spec reason))
                    (is (= specs-errors details)))))))
          (testing "and return models"
            ; when
            (let [expected-existing (gen/sample (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  feed (gen/generate (s/gen :rss-feed-reader.core.feed.logic/model))
                  dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  logic-model (gen/sample (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                            rss-feed-reader.core.feed.item.logic/get-by-links (fn [_] expected-existing)
                            rss-feed-reader.core.feed.logic/get-by-id (fn [_] feed)
                            rss-feed-reader.core.feed.item.logic/logic-create-models->dao-models (fn [_] dao-models)
                            rss-feed-reader.core.feed.item.dao/insert-multi (fn [_] dao-models)
                            rss-feed-reader.core.feed.item.logic/dao-model->logic-model (fn [_ _] logic-model)]
                ; then
                (let [actual (create-multi create-models)]
                  (= actual (merge expected-existing expected)))))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))]
      ; when
      (let [expected (gen/generate (s/gen int?))]
        (with-redefs [rss-feed-reader.core.feed.item.dao/delete (fn [_] expected)]
          ; then
          (let [actual (delete model)]
            (is (= actual expected))))))))