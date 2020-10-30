(ns rss-feed-reader.web.middleware
  (:require [rss-feed-reader.utils.cid :as cid]
            [clojure.tools.logging :as log]
            [rss-feed-reader.utils.time :as t]
            [rss-feed-reader.utils.map :as maps]
            [rss-feed-reader.web.response :as response]))

(defn wrap-logger [handler]
  (fn [request]
    (let [{:keys [uri request-method]} request
          from (t/instant->long (t/instant-now))]
      (cid/set-new)
      (log/info "[REQ]" request-method uri)
      (let [response (handler request)
            {:keys [status]} response
            to (t/instant->long (t/instant-now))]
        (log/info "[RES]" (format "%dms" (- to from)) status)
        response))))

(defn wrap-server-error [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        (response/server-error {:cid (cid/get)})))))

(defn wrap-json-response-body [handler]
  (fn [request]
    (let [response (handler request)
          resp-body (:body response)]
      (if (nil? resp-body)
        response
        (->> (maps/->unq-map resp-body)
             (assoc response :body))))))
