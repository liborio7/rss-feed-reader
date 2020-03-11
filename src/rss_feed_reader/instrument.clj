(ns rss-feed-reader.instrument
  (:require [rss-feed-reader.env :refer [env]]
            [orchestra.spec.test :as orchestra]))

(case (:environment env)
  "dev"
  (do
    (orchestra/instrument))
  "test"
  (do
    (orchestra/instrument))
  (do
    ; nothing to do here
    ))

