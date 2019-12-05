(ns rss-feed-reader.api.feed.router
  (:require [rss-feed-reader.api.feed.handler :as h]))

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :post h/create-feed}]
    ["/:id" {:name   ::feeds-id
             :get    h/get-feed
             :delete h/delete-feed
             }]]])