(ns rss-feed-reader.utils.cid
  (:refer-clojure :exclude [get set])
  (:import (org.slf4j MDC)))

(defn- generate []
  (apply str (take 7 (repeatedly #(char (+ (rand 26) 65))))))

(defn set [cid]
  (MDC/put "cid" cid))

(defn set-new []
  (set (generate)))

(defn get []
  (MDC/get "cid"))