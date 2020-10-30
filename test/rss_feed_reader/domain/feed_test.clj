(ns rss-feed-reader.domain.feed-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.feed :refer :all]
            [rss-feed-reader.system_test :refer [with-system]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [com.stuartsierra.component :as component]
            [rss-feed-reader.db.provider.h2 :as datasource]
            [rss-feed-reader.db.migration :as migration])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))

(defn system-map []
  (component/system-map
    :datasource (datasource/map->H2Datasource {:config   {:h2-jdbc (str "jdbc:h2:mem:feed-test-"
                                                                        (gen/generate (s/gen string?)))}
                                               :on-start #(migration/migrate %)
                                               :on-stop  #(migration/rollback %)})
    :feeds (component/using
             (map->DbFeeds {})
             [:datasource])
    ))

; missing get-all test

(deftest should-get
  (testing "should get"
    (with-system [system (system-map)]
      ; given
      (testing "by"
        (let [feeds (:feeds system)
              model (gen/generate (s/gen :rss-feed-reader.domain.feed/model))
              model (create! feeds model)]
          (testing "id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-id feeds {:feed.domain/id (UUID/randomUUID)})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-id feeds model)]
                ; then
                (is (= model actual)))))
          (testing "ids"
            (testing "and return models"
              ; when
              (let [actual (get-by-ids feeds [model])]
                ; then
                (is (= [model] actual)))))
          (testing "link"
            (testing "and return nil"
              ; when
              (let [unexisting-link (gen/generate (s/gen :feed.domain/link))
                    actual (get-by-link feeds {:feed.domain/link unexisting-link})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-link feeds model)]
                ; then
                (is (= model actual))))))))))

(deftest should-create
  (testing "should create"
    (with-system [system (system-map)]
      ; given
      (let [feeds (:feeds system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.feed/create-model))]
        (testing "throwing specs exception"
          ; when
          (try
            (create! feeds {})
            (do-report {:type     :fail,
                        :message  "uncaught exception",
                        :expected ExceptionInfo,
                        :actual   nil})
            (catch ExceptionInfo e
              ; then
              (let [data (ex-data e)
                    {:keys [cause reason details]} data]
                (is (= :feed-create cause))
                (is (= :invalid-spec reason))
                (is (not-empty details))))))
        (testing "and return existing model"
          ; when
          (let [model (create! feeds create-model)
                actual (create! feeds create-model)]
            ; then
            (is (= model actual))))
        (testing "and return new model"
          ; when
          (let [actual (create! feeds create-model)]
            ; then
            (is (= create-model (select-keys actual (keys create-model))))
            (is (= 0 (:feed.domain/version actual)))
            (is (not (nil? (:feed.domain/order-id actual))))
            (is (not (nil? (:feed.domain/insert-time actual))))
            (is (nil? (:feed.domain/update-time actual)))))))))

(deftest should-delete
  (testing "should delete"
    (with-system [system (system-map)]
      ; given
      (let [feeds (:feeds system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.feed/create-model))
            model (create! feeds create-model)]
        ; when
        (let [actual (delete! feeds model)]
          ; then
          (is (= 1 actual))
          (is (nil? (get-by-id feeds model))))))))