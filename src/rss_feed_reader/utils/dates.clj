(ns rss-feed-reader.utils.dates
  (:require [clj-time.format :as f]
            [clojure.tools.logging :as log]))

(def date-formatter (f/formatter "yyyy-MM-dd"))
(def date-time-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))

(defn parse-date [s]
  (try
    (f/parse date-formatter s)
    (catch Exception _
      (log/warn "fail to parse date" s))))

(defn unparse-date [inst]
  (try
    (f/unparse date-formatter inst)
    (catch Exception _
      (log/warn "fail to unparse date" inst))))

(defn parse-date-time [s]
  (try
    (f/parse date-time-formatter s)
    (catch Exception _
      (log/warn "invalid date" s))))