(ns rss-feed-reader.api.feeds.router
  (:require [rss-feed-reader.api.feeds.handler :as h]))

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :post h/create-feed}]
    ["/:id" {:name   ::feeds-id
             :get    h/get-feed
             :delete h/delete-feed
             }]]])