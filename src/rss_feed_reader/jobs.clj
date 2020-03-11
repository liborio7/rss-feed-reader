(ns rss-feed-reader.jobs
  (:require [rss-feed-reader.env :refer [env]]
            [overtone.at-at :as j]
            [rss-feed-reader.core.feed.item.job :as feed-item-job]))

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
