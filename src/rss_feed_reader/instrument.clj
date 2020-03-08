(ns rss-feed-reader.instrument
  (:require [environ.core :refer [env]]
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

