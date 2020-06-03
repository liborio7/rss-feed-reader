(ns rss-feed-reader.api.router
  (:require [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [reitit.ring :as ring]
            [rss-feed-reader.env :refer [env]]
            [rss-feed-reader.api.middleware :refer :all]
            [rss-feed-reader.api.accounts.router :as accounts]
            [rss-feed-reader.api.feeds.router :as feeds]))

(def routes [accounts/routes
             feeds/routes])

(def middlewares
  (let [m [[wrap-logger]
           [wrap-server-error]
           [wrap-params]
           [wrap-keyword-params]
           [wrap-json-body {:keywords? true :bigdecimals? true}]
           [wrap-json-response {:pretty true}]
           [wrap-json-response-body]]]
    (case (:environment env)
      "repl"
      (into [[wrap-cors
              :access-control-allow-origin [#".*"]
              :access-control-allow-methods [:get :post :put :delete]]]
            m)
      "dev"
      (into [[wrap-cors
              :access-control-allow-origin [#".*"]
              :access-control-allow-methods [:get :post :put :delete]]]
            m)
      m)))

(def handler
  (ring/ring-handler (ring/router routes)
                     (ring/create-default-handler)
                     {:middleware middlewares}))
