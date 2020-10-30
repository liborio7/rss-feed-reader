(ns rss-feed-reader.domain.job-test
  (:require [clojure.test :refer :all]
            [rss-feed-reader.domain.job :refer :all]
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
    :datasource (datasource/map->H2Datasource {:config   {:h2-jdbc (str "jdbc:h2:mem:job-test-"
                                                                        (gen/generate (s/gen string?)))}
                                               :on-start #(migration/migrate %)
                                               :on-stop  #(migration/rollback %)})
    :jobs (component/using
            (map->DbJobs {})
            [:datasource])
    ))

; missing updates tests

(deftest should-get
  (testing "should get"
    (with-system [system (system-map)]
      ; given
      (testing "by"
        (let [jobs (:jobs system)
              model (gen/generate (s/gen :rss-feed-reader.domain.job/model))
              model (create! jobs model)]
          (testing "id"
            (testing "and return nil"
              ; when
              (let [actual (get-by-id jobs {:job.domain/id (UUID/randomUUID)})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-id jobs model)]
                ; then
                (is (= model actual)))))
          (testing "name"
            (testing "and return nil"
              ; when
              (let [actual (get-by-name jobs {:job.domain/name (gen/generate (s/gen :job.domain/name))})]
                ; then
                (is (nil? actual))))
            (testing "and return model"
              ; when
              (let [actual (get-by-name jobs model)]
                ; then
                (is (= model actual))))))))))

(deftest should-create
  (testing "should create"
    (with-system [system (system-map)]
      ; given
      (let [jobs (:jobs system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.job/create-model))]
        (testing "throwing specs exception"
          ; when
          (try
            (create! jobs {})
            (do-report {:type     :fail,
                        :message  "uncaught exception",
                        :expected ExceptionInfo,
                        :actual   nil})
            (catch ExceptionInfo e
              ; then
              (let [data (ex-data e)
                    {:keys [cause reason details]} data]
                (is (= :job-create cause))
                (is (= :invalid-spec reason))
                (is (not-empty details))))))
        (testing "and return existing model"
          ; when
          (let [model (create! jobs create-model)
                actual (create! jobs create-model)]
            ; then
            (is (= model actual))))
        (testing "and return new model"
          ; when
          (let [actual (create! jobs create-model)]
            ; then
            (is (= create-model (select-keys actual (keys create-model))))
            (is (= 0 (:job.domain/version actual)))
            (is (not (nil? (:job.domain/order-id actual))))
            (is (not (nil? (:job.domain/insert-time actual))))
            (is (nil? (:job.domain/update-time actual)))))))))

(deftest should-delete
  (testing "should delete"
    (with-system [system (system-map)]
      ; given
      (let [jobs (:jobs system)
            create-model (gen/generate (s/gen :rss-feed-reader.domain.job/create-model))
            model (create! jobs create-model)]
        ; when
        (let [actual (delete! jobs model)]
          (is (= 1 actual))
          (is (nil? (get-by-id jobs model))))))))