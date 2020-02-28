(ns rss-feed-reader.jobs
  (:require [environ.core :refer [env]]
            [overtone.at-at :as j]
            [rss-feed-reader.job.feed_item :as feed-item-job]))

(def my-pool (j/mk-pool))
(case (:environment env)
  "dev"
  (do
    (j/every 15000 feed-item-job/run my-pool :initial-delay 5000))
  "test"
  (do
    ; nothing to do here
    )
  "testing"
  (do
    (j/every 15000 feed-item-job/run my-pool :initial-delay 5000))
  "staging"
  (do
    (j/every 15000 feed-item-job/run my-pool :initial-delay 5000))
  "production"
  (do
    (j/every 15000 feed-item-job/run my-pool :initial-delay 5000)))
