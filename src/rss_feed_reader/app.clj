(ns rss-feed-reader.app
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [reitit.ring :as ring]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.feed.router :as feed]))

(defn wrap-logger [handler]
  (fn [request]
    (let [{:keys [uri request-method body]} request
          from (tc/to-long (t/now))]
      (log/info "[REQ]" request-method uri body)
      (let [response (handler request)
            {:keys [status body]} response
            to (tc/to-long (t/now))]
        (log/info (format "[RES]") (format "%dms" (- to from)) status body)
        response))))

(defn wrap-json-response-body [handler]
  (fn [request]
    (let [response (handler request)
          resp-body (:body response)]
      (if (nil? resp-body)
        response
        (->> (maps/->unq-map resp-body)
             (assoc response :body))))))

(def app
  (ring/ring-handler
    (ring/router [feed/routes])
    (ring/create-default-handler)
    {:middleware [
                  [wrap-logger]
                  [json/wrap-json-body {:keywords? true :bigdecimals? true}]
                  [json/wrap-json-response {:pretty true}]
                  [wrap-json-response-body]
                  ]}))

(defn -main [& _args]
  (jetty/run-jetty app {:port 3000}))