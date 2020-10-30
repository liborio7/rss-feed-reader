(ns rss-feed-reader.env
  (:require [environ.core :as environ]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn read-resources []
  (->> ["postgres.edn" "scheduler.edn" "telegram.edn"]
       (map io/resource)
       (filter identity)
       (map slurp)
       (map edn/read-string)
       (into {})))

(defn read-environ []
  (into {} environ/env))

(defn load-env []
  (merge
    (read-resources)
    (read-environ)))

(defonce env (load-env))