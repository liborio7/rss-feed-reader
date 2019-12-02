(ns rss-feed-reader.feed.router
  (:require [rss-feed-reader.feed.handler :as h]))

(def routes
  [["/feeds"
    ["" {:name ::feeds
         :post h/post}]
    ["/:id" {:name   ::feeds-id
             :get    h/get-by-id
             :delete h/delete
             }]]])