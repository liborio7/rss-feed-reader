(ns rss-feed-reader.rss.parser
  (:require [clojure.tools.logging :as log]
            [clojure.xml :as xml]))

(defn parse [link]
  (log/trace "parse" link)
  (try
    (->> link
         (str)
         (xml/parse)
         (:content)
         (first)
         (:content)
         (filter (comp #{:item} :tag))
         (map :content)
         (map (fn [item] (reduce #(assoc %1 (:tag %2) (:content %2)) {} item))))
    (catch Exception _
      '())))
