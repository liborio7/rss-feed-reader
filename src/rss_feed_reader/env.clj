(ns rss-feed-reader.env
  (:require [environ.core :as environ]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- read-env-resources []
  (->> (environ/env :environment)
       (format "resources/%s")
       (io/file)
       (file-seq)
       (filter #(clojure.string/ends-with? (.getName %) ".edn"))
       (map slurp)
       (map edn/read-string)
       (reduce into)))

(defn- read-environ []
  (into {} environ/env))

(def env
  (merge
    (read-env-resources)
    (read-environ)))
