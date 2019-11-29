(ns rss-feed-reader.handler.utils
  (:require [clojure.spec.alpha :as s])
  (:import (java.util UUID)))

(defn string->uuid [s]
  (try
    (UUID/fromString s)
    (catch Exception _ nil)))

(defn map->ns-map [ns m]
  (->> (map (fn [[key value]] [(keyword ns (name key)) value]) m)
       (into {})))

(s/fdef map->ns-map
        :args (s/cat :ns string? :m map?)
        :ret map?)

(defn ns-map->map [m]
  (->> (map (fn [[key value]] [(keyword (name key)) value]) m)
       (into {})))

(s/fdef ns-map->map
        :args (s/cat :m map?)
        :ret map?)

(defn wrap-resp-body-ns [f request]
  (let [response (f request)
        resp-body (:body response)]
    (->> (ns-map->map resp-body)
         (assoc response :body))))

(s/fdef wrap-resp-body-ns
        :args (s/cat :f fn? :request map?)
        :ret map?)

(defn wrap-body-ns [ns f request]
  (->> (:body request)
       (map->ns-map ns)
       (assoc request :body)
       (wrap-resp-body-ns f)))

(s/fdef wrap-body-ns
        :args (s/cat :ns string? :f fn? :request map?)
        :ret map?)