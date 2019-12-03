(ns rss-feed-reader.utils.uri
  (:require [clojure.java.io :as jio]
            [clojure.spec.alpha :as s])
  (:import (java.net URI)))

(defn from-string [s]
  (try
    (-> (jio/as-url s)
        .toString
        URI.)
    (catch Exception _)))

(s/fdef from-string
        :args (s/cat :s string?)
        :ret (s/or :ok uri? :err nil?))
