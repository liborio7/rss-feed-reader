(ns rss-feed-reader.app
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :as json]
            [reitit.ring :as ring]
            [clojure.tools.logging :as log]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.api.feed.router :as feed]
            [rss-feed-reader.api.account.router :as account]))

(defn wrap-logger [handler]
  (fn [request]
    (let [{:keys [uri request-method]} request
          from (tc/to-long (t/now))]
      (log/info "[REQ]" request-method uri)
      (let [response (handler request)
            {:keys [status body]} response
            to (tc/to-long (t/now))]
        (log/info (format "[RES]") (format "%dms" (- to from)) status body)
        response))))

(defn wrap-server-error [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (r/server-error)))))

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
    (ring/router [feed/routes
                  account/routes])
    (ring/create-default-handler)
    {:middleware [
                  [wrap-logger]
                  [wrap-server-error]
                  [json/wrap-json-body {:keywords? true :bigdecimals? true}]
                  [json/wrap-json-response {:pretty true}]
                  [wrap-json-response-body]
                  ]}))

(defn -main [& _args]
  (jetty/run-jetty app {:port 3000}))