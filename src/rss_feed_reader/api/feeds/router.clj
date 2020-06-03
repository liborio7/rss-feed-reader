(ns rss-feed-reader.api.feeds.router
  (:require [rss-feed-reader.api.feeds.handler :refer :all]))

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :get  get-feeds
         :post create-feed}]
    ["/:feed-id" {:name   ::feeds-id
                  :get    get-feed
                  :delete delete-feed
                  }]
    ["/:feed-id/items" {:name ::feed-items
                        :get  get-feed-items
                        }]
    ]])