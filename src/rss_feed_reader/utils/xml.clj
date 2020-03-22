(ns rss-feed-reader.utils.xml
  (:require [clojure.xml :as xml]))

(defn parse [s]
  (try
    (xml/parse s)
    (catch Exception _
      nil)))