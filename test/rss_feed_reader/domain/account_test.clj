(ns rss-feed-reader.domain.account-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.account :refer :all]
            [rss-feed-reader.system_test :refer [with-system with-system-1]]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]
            [rss-feed-reader.db.provider.h2 :as datasource]
            [com.stuartsierra.component :as component]
            [rss-feed-reader.db.migration :as migration])
  (:import (clojure.lang ExceptionInfo)
           (java.util UUID)))

(defn system-map []
  (component/system-map
    :datasource (datasource/map->H2Datasource {:config   {:h2-jdbc (str "jdbc:h2:mem:account-test-"
                                                                        (gen/generate (s/gen string?)))}
                                               :on-start #(migration/migrate %)
                                               :on-stop  #(migration/rollback %)})
    :accounts (component/using
                (map->DbAccounts {})
                [:datasource])
    ))

(deftest should-get
  (testing "should get"
    (with-system [system (system-map)]
      ; given
      (let [accounts (:accounts system)
            model (gen/generate (s/gen :rss-feed-reader.domain.account/model))
            model (create! accounts model)]
        (testing "all"
          ; when
          (let [actual (get-all accounts)]
            ; then
            (is (= [model] actual))))
        (testing "by"
          (testing "id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-id accounts {:account.domain/id (UUID/randomUUID)})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-id accounts model)]
                ; then
                (is (= model actual)))))
          (testing "ids"
            (testing "and return models"
              ; when
              (let [actual (get-by-ids accounts [model])]
                ; then
                (is (= [model] actual)))))
          (testing "chat id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-chat-id accounts {:account.domain/chat-id (inc (:account.domain/chat-id model))})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-chat-id accounts model)]
                ; then
                (is (= model actual))))))))))

(deftest should-create
  (testing "should create"
    (with-system [system (system-map)]
      ; given
      (let [accounts (:accounts system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.account/create-model))]
        (testing "throwing specs exception"
          ; when
          (try
            (create! accounts {})
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
          (let [model (create! accounts create-model)
                actual (create! accounts create-model)]
            ; then
            (is (= model actual))))
        (testing "and return new model"
          ; when
          (let [actual (create! accounts create-model)]
            ; then
            (is (= create-model (select-keys actual (keys create-model))))
            (is (= 0 (:account.domain/version actual)))
            (is (not (nil? (:account.domain/order-id actual))))
            (is (not (nil? (:account.domain/insert-time actual))))
            (is (nil? (:account.domain/update-time actual)))))))))

(deftest should-delete
  (testing "should delete"
    (with-system [system (system-map)]
      ; given
      (let [accounts (:accounts system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.account/create-model))
            model (create! accounts create-model)]
        ; when
        (let [actual (delete! accounts model)]
          ; then
          (is (= 1 actual))
          (is (nil? (get-by-id accounts model))))))))