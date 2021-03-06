(ns rss-feed-reader.utils.uuid
  (:require [clojure.spec.alpha :as s])
  (:import (java.util UUID)))

(defn from-string [s]
  (try
    (UUID/fromString s)
    (catch Exception _ nil)))