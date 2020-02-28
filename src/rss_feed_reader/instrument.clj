(ns rss-feed-reader.instrument
  (:require [environ.core :refer [env]]
            [clojure.spec.test.alpha :as stest]))

(case (:environment env)
  "dev"
  (do
    (stest/instrument))
  "test"
  (do
    (stest/instrument))
  (do
    ; nothing to do here
    ))

