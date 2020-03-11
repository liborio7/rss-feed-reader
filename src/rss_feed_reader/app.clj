(ns rss-feed-reader.app
  (:require [rss-feed-reader.instrument]
            [rss-feed-reader.jobs]
            [rss-feed-reader.env :refer [env]]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.params :as params]
            [ring.middleware.keyword-params :as kw-params]
            [ring.middleware.json :as json]
            [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [reitit.ring :as ring]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.utils.response :as r]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.core.feed.router :as feed]
            [rss-feed-reader.core.account.router :as account]))

(defn wrap-logger [handler]
  (fn [request]
    (let [{:keys [uri request-method]} request
          from (tc/to-long (t/now))]
      (cid/set-new)
      (log/info "[REQ]" request-method uri)
      (let [response (handler request)
            {:keys [status]} response
            to (tc/to-long (t/now))]
        (log/info "[RES]" (format "%dms" (- to from)) status)
        response))))

(defn wrap-server-error [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (r/server-error {:cid (cid/get)})))))

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
                  [params/wrap-params]
                  [kw-params/wrap-keyword-params]
                  [json/wrap-json-body {:keywords? true :bigdecimals? true}]
                  [json/wrap-json-response {:pretty true}]
                  [wrap-json-response-body]
                  ]}))

(defn -main [& _args]
  (cid/set-new)
  (log/info "environment:" (:environment env))
  (jetty/run-jetty app {:port 3000}))