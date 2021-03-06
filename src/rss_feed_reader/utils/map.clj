(ns rss-feed-reader.utils.map
  (:require [clojure.spec.alpha :as s]))

(defn ->q-map [ns m]
  (->> (map (fn [[key value]] [(keyword ns (name key)) value]) m)
       (into {})))

(s/fdef ->q-map
        :args (s/cat :ns string? :m map?)
        :ret map?)

(defn ->unq-map [m]
  (->> (map (fn [[key value]]
              [(keyword (name key))
               (cond
                 (map? value) (->unq-map value)
                 (coll? value) (map (fn [el] (if (map? el) (->unq-map el) el)) value)
                 :else value)]) m)
       (into {})))

(s/fdef ->unq-map
        :args (s/cat :m map?)
        :ret map?)