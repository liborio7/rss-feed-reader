(ns rss-feed-reader.db.postgres-test
  (:require [rss-feed-reader.db.postgres :refer :all]
            [mount.core :refer [defstate]]
            [hikari-cp.core :as hikari]
            [rss-feed-reader.env :refer [env]]
            [clojure.java.jdbc :as jdbc]
            [mount.core :as mount]))

(defn do! [ds stm]
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
  (let [ds (start-ds)
        db (random-db-name)]
    (do! ds (str "CREATE DATABASE " db ";"))
    (let [test-ds (start-ds (test-config db))]
      (mount/start-with {#'rss-feed-reader.db.postgres/ds test-ds})
      (migrate (ragtime-config test-ds))
      (f)
      (mount/stop)
      (hikari/close-datasource test-ds)
      (do! ds (str "REVOKE CONNECT ON DATABASE " db " FROM public;"))
      (do! ds (str "DROP DATABASE " db ";"))
      (hikari/close-datasource ds))))