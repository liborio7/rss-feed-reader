(ns rss-feed-reader.db.datasource-test
  (:require [rss-feed-reader.db.datasource :refer :all]
            [mount.core :refer [defstate]]
            [rss-feed-reader.env :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]))

(defn db-do! [ds stm]
  (jdbc/with-db-connection [conn {:datasource ds}]
                           (jdbc/db-do-commands conn false stm)))

(defn test-config [db]
  (config {:username      (:postgres-user env)
           :password      (:postgres-password env)
           :database-name db
           :server-name   (:postgres-host env)
           :port-number   (:postgres-port env)}))

(defn random-db-name []
  (str "test" (-> (java.util.UUID/randomUUID)
                  (str)
                  (clojure.string/replace "-" ""))))

(defn db-fixture [f]
  (with-open [ds (start-ds)]
    (let [db (random-db-name)]
      (db-do! ds (str "CREATE DATABASE " db ";"))
      (with-open [test-ds (start-ds (test-config db))]
        (mount/start-with {#'rss-feed-reader.db.datasource/ds test-ds})
        (migrate (ragtime-config test-ds))
        (f)
        (mount/stop))
      (db-do! ds (str "REVOKE CONNECT ON DATABASE " db " FROM public;"))
      (db-do! ds (str "DROP DATABASE " db ";")))))