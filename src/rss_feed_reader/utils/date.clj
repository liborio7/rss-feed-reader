(ns rss-feed-reader.utils.date
  (:require [clj-time.format :as f]
            [clojure.tools.logging :as log]))

(def date-formatter (f/formatter "yyyy-MM-dd"))
(def date-time-formatter (f/formatter "yyyy-MM-dd HH:mm:ss"))

(defn parse-date
  ([s] (parse-date s date-formatter))
  ([s formatter]
   (try
     (f/parse formatter s)
     (catch Exception _
       (log/warn "fail to parse date" s)))))

(defn unparse-date
  ([inst] (unparse-date inst date-formatter))
  ([inst formatter]
   (try
     (f/unparse formatter inst)
     (catch Exception _
       (log/warn "fail to unparse date" inst)))))

(defn parse-date-time
  ([s] (parse-date-time s date-time-formatter))
  ([s formatter]
   (try
     (f/parse formatter s)
     (catch Exception _
       (log/warn "invalid date" s)))))