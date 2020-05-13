(ns rss-feed-reader.utils.spec
  (:require [clojure.spec.alpha :as s]))

(defn describe-problem [problem]
  (let [path (:path problem)
        pred (:pred problem)
        pred (if (coll? pred)
               ((comp (juxt first last) last) pred)
               pred)]
    (->> [path pred]
         (flatten)
         (map name))))

(defn errors [spec data]
  (->> (s/explain-data spec data)
       (:clojure.spec.alpha/problems)
       (map describe-problem)))