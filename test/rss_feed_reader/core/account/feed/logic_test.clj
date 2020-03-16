(ns rss-feed-reader.core.account.feed.logic-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.core.account.feed.logic :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s])
  (:import (clojure.lang ExceptionInfo)))

(deftest should-get
  (testing "should get"
    ; given
    (testing "by"
      (let [model (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
        (testing "id"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-id (fn [_] nil)]
              ; then
              (let [actual (get-by-id model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.feed.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
              (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-id (fn [_] dao-model)
                            rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-id model)]
                  (is (= actual expected)))))))
        (testing "account"
          (testing "with provided arguments"
            ; given
            (let [starting-after (gen/generate (s/gen nat-int?))
                  limit (gen/generate (s/gen nat-int?))]
              ; when
              (let [dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                    logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                    expected (repeat (count dao-models) logic-model)]
                (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-account-id
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
                              rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_ _ _] logic-model)]
                  ; then
                  (let [actual (get-by-account model :starting-after starting-after :limit limit)]
                    (is (= actual expected)))))))
          (testing "with default arguments"
            ; given
            (let [dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-account-id
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
                            rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_ _ _] logic-model)]
                ; then
                (let [actual (get-by-account model)]
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
                (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-feed-id
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
                              rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_ _ _] logic-model)]
                  ; then
                  (let [actual (get-by-feed model :starting-after starting-after :limit limit)]
                    (is (= actual expected)))))))
          (testing "with default arguments"
            ; given
            (let [dao-models (gen/sample (s/gen :rss-feed-reader.core.feed.item.dao/model))
                  logic-model (gen/generate (s/gen :rss-feed-reader.core.feed.item.logic/model))
                  expected (repeat (count dao-models) logic-model)]
              (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-feed-id
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
                            rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_ _ _] logic-model)]
                ; then
                (let [actual (get-by-feed model)]
                  (is (= actual expected)))))))
        (testing "account and feed"
          (testing "and return nil"
            ; when
            (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-account-id-and-feed-id (fn [_] nil)]
              ; then
              (let [actual (get-by-account-and-feed model)]
                (is (nil? actual)))))
          (testing "and return model"
            ; when
            (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.feed.dao/model))
                  expected (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
              (with-redefs [rss-feed-reader.core.account.feed.dao/get-by-account-id-and-feed-id (fn [_] dao-model)
                            rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_] expected)]
                ; then
                (let [actual (get-by-account-and-feed model)]
                  (is (= actual expected)))))))))))

(deftest should-create
  (testing "should create"
    ; given
    (let [create-model (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/create-model))]
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
                  (is (= :account-feed-logic-create cause))
                  (is (= :invalid-spec reason))
                  (is (= specs-errors details)))))))
        (testing "and return existing model"
          ; when
          (let [expected (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.account.feed.logic/get-by-account-and-feed (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (is (= actual expected))))))
        (testing "and return new model"
          ; when
          (let [dao-model (gen/generate (s/gen :rss-feed-reader.core.account.feed.dao/model))
                expected (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
            (with-redefs [rss-feed-reader.utils.spec/errors (fn [_ _] {})
                          rss-feed-reader.core.account.feed.logic/get-by-account-and-feed (fn [_] nil)
                          rss-feed-reader.core.account.feed.logic/logic-create-model->dao-model (fn [_] dao-model)
                          rss-feed-reader.core.account.feed.dao/insert (fn [_] dao-model)
                          rss-feed-reader.core.account.feed.logic/dao-model->logic-model (fn [_] expected)]
              ; then
              (let [actual (create create-model)]
                (is (= actual expected))))))))))

(deftest should-delete
  (testing "should delete"
    ; given
    (let [model (gen/generate (s/gen :rss-feed-reader.core.account.feed.logic/model))]
      ; when
      (let [expected (gen/generate (s/gen int?))]
        (with-redefs [rss-feed-reader.core.account.feed.dao/delete (fn [_] expected)]
          ; then
          (let [actual (delete model)]
            (is (= actual expected))))))))