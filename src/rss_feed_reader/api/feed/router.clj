(ns rss-feed-reader.api.feed.router
  (:require [rss-feed-reader.api.feed.handler :as h]))

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :post h/create-feed}]
    ["/:feed-id" {:name   ::feeds-id
                  :get    h/get-feed
                  :delete h/delete-feed
                  }]
    ["/:feed-id/items" {:name ::feed-items
                        :get  h/get-feed-items
                        }]
    ]])