(ns rss-feed-reader.utils.spec
  (:require [clojure.spec.alpha :as s]))

(defn errors [spec data]
  (->> (s/explain-data spec data)
       :clojure.spec.alpha/problems
       (map (fn [m] [(:path m) (:pred m)]))))