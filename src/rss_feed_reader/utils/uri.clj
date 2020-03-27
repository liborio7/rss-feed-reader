(ns rss-feed-reader.utils.uri
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as s])
  (:import (java.net URI)))

(defn from-string [s]
  (try
    (-> (io/as-url s)
        .toString
        URI.)
    (catch Exception _)))

(s/fdef from-string
        :args (s/cat :s string?)
        :ret (s/or :ok uri? :err nil?))
