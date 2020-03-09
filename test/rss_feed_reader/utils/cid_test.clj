(ns rss-feed-reader.utils.cid-test
  (:refer-clojure :exclude [get set])
  (:require [clojure.test :refer :all]
            [rss-feed-reader.utils.cid :refer :all]
            [clojure.spec.gen.alpha :as gen]
            [clojure.spec.alpha :as s]))

(deftest should-generate
  (testing "should generate"
    ; when
    (let [cid (generate)]
      ; then
      (is (s/valid? :utils/cid cid)))))

(deftest should-set-and-get
  (testing "should set and get"
    (testing "with cid"
      ; given
      (let [expected (gen/generate (s/gen :utils/cid))]
        ; when
        (set expected)
        ; then
        (let [actual (get)]
          (is (= actual expected)))))
    (testing "without cid"
      ; when
      (set-new)
      ; then
      (let [cid (get)]
        (is (s/valid? :utils/cid cid))))))