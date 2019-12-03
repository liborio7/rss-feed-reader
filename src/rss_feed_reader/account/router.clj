(ns rss-feed-reader.account.router
  (:require [rss-feed-reader.account.handler :as h]))

(def routes
  [["/accounts"
    ["" {:name ::accounts
         :post h/post}]
    ["/:id" {:name   ::accounts-id
             :get    h/get-by-id
             :delete h/delete
             }]]])