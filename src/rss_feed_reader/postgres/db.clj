(ns rss-feed-reader.postgres.db
  (:require [clojure.edn :as edn]
            [environ.core :refer [env]]))

(def connection
  (let [props (->> (:environment env)
                   (format "resources/%s/db.edn")
                   (slurp)
                   (edn/read-string))]
    {:dbtype   "postgresql"
     :dbname   (:database props)
     :host     (:host props)
     :user     (:user props)
     :password (:password props)}))