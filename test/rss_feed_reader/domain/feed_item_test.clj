(ns rss-feed-reader.domain.feed-item-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.feed-item :refer :all]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.system_test :refer [with-system]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.db.provider.h2 :as datasource]
            [rss-feed-reader.db.migration :as migration])
  (:import (clojure.lang ExceptionInfo)))

(defn system-map []
  (component/system-map
    :datasource (datasource/map->H2Datasource {:config   {:h2-jdbc (str "jdbc:h2:mem:feed-item-test-"
                                                                        (gen/generate (s/gen string?)))}
                                               :on-start #(migration/migrate %)
                                               :on-stop  #(migration/rollback %)})
    :feeds (component/using
             (feed/map->DbFeeds {})
             [:datasource])

    :feeds-items (component/using
                   (map->DbFeedsItems {})
                   [:datasource :feeds])
    ))

(deftest should-get
  (testing "should get"
    (with-system [system (system-map)]
      ; given
      (testing "by"
        (let [feeds (:feeds system)
              feeds-items (:feeds-items system)
              feed (feed/create! feeds (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
              model (-> (gen/generate (s/gen :rss-feed-reader.domain.feed-item/model))
                        (assoc :feed.item.domain/feed feed)
                        ((partial create! feeds-items)))]
          (testing "id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-id feeds-items {:feed.item.domain/id (java.util.UUID/randomUUID)})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-id feeds-items model)]
                ; then
                (is (= model actual)))))
          (testing "feed"
            (testing "and return models"
              ; when
              (let [actual (get-by-feed feeds-items model)]
                ; then
                (is (= (seq [model]) actual))))
            (let [actual (get-by-feed feeds-items {:feed.item.domain/feed {:feed.domain/id (java.util.UUID/randomUUID)}})]
              ; then
              (is (empty? actual))))
          (testing "link"
            (testing "and return nil"
              ; when
              (let [actual (get-by-link feeds-items {:feed.item.domain/link (gen/generate (s/gen :feed.item.domain/link))})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-link feeds-items model)]
                ; then
                (is (= model actual)))))
          (testing "links"
            (testing "and return models"
              ; when
              (let [actual (get-by-links feeds-items [model])]
                ; then
                (is (= (seq [model]) actual))))))))))

(deftest should-create
  (testing "should create"
    (with-system [system (system-map)]
      ; given
      (let [feeds (:feeds system)
            feeds-items (:feeds-items system)
            feed (feed/create! feeds (gen/generate (s/gen :rss-feed-reader.domain.feed/model)))
            create-model (-> (gen/generate (s/gen :rss-feed-reader.domain.feed-item/create-model))
                             (assoc :feed.item.domain/feed feed))]
        (testing "throwing specs exception"
          ; when
          (try
            (create! feeds-items {})
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
          (let [expected (create! feeds-items create-model)
                actual (create! feeds-items create-model)]
            ; then
            (is (= expected actual))))
        (testing "and return new model"
          ; when
          (let [actual (create! feeds-items create-model)]
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
              (create-multi! feeds-items [{}])
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
            (let [expected (create-multi! feeds-items [create-model])
                  actual (create-multi! feeds-items [create-model])]
              ; then
              (is (= expected actual))))
          (testing "and return new models"
            ; when
            (let [actual (create-multi! feeds-items [create-model])
                  actual (first actual)]
              ; then
              (is (= 0 (:feed.item.domain/version actual)))
              (is (not (nil? (:feed.item.domain/order-id actual))))
              (is (not (nil? (:feed.item.domain/insert-time actual))))
              (is (nil? (:feed.item.domain/update-time actual))))))))))

(deftest should-delete
  (testing "should delete"
    (with-system [system (system-map)]
      ; given
      (let [feeds-items (:feeds-items system)
            model (create! feeds-items (gen/generate (s/gen :rss-feed-reader.domain.feed-item/create-model)))]
        ; when
        (let [actual (delete! feeds-items model)]
          ; then
          (is (= 1 actual))
          (is (nil? (get-by-id feeds-items model))))))))