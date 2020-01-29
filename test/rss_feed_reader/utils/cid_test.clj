(ns rss-feed-reader.utils.cid-test
  (:refer-clojure :exclude [get set])
  (:require [clojure.test :refer :all]
            [rss-feed-reader.utils.cid :refer :all]
            [clojure.spec.alpha :as s]
            [clojure.spec.test.alpha :as stest]
            [clojure.spec.gen.alpha :as gen]))

(deftest should-set-and-get
  (testing "generate"
    (stest/instrument)
    ; given
    (let [actual (gen/generate (s/gen :utils/cid))]
      (set actual)
      ; when
      (let [expected (get)]
        ; then
        (is (= expected actual))))))
