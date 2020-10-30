(ns rss-feed-reader.web.router
  (:require [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.ring :as ring]
            [rss-feed-reader.web.middleware :refer :all]
            [rss-feed-reader.web.handler.accounts :as accounts]
            [rss-feed-reader.web.handler.feeds :as feeds]))

(defn routes [accounts accounts-feeds feeds feeds-items]
  [(accounts/routes accounts accounts-feeds feeds)
   (feeds/routes feeds feeds-items)])

(defn middlewares []
  [[wrap-logger]
   [wrap-server-error]
   [wrap-params]
   [wrap-keyword-params]
   [wrap-json-body {:keywords? true :bigdecimals? true}]
   [wrap-json-response {:pretty true}]
   [wrap-json-response-body]])

(defn handler [accounts accounts-feeds feeds feeds-items]
  (ring/ring-handler (ring/router (routes accounts accounts-feeds feeds feeds-items))
                     (ring/create-default-handler)
                     {:middleware (middlewares)}))
