(ns rss-feed-reader.api.account.router
  (:require [rss-feed-reader.api.account.handler :as h]))

(def routes
  [["/accounts"
    ["" {:name ::accounts
         :post h/create-account}]
    ["/:account-id" {:name   ::account
                     :get    h/get-account
                     :delete h/delete-account
                     }]
    ["/:account-id/feeds" {:name ::account-feeds
                           :post h/create-account-feed
                           }]
    ["/:account-id/feeds/:account-feed-id" {:name   ::account-feed
                                            :get    h/get-account-feed
                                            :delete h/delete-account-feed
                                            }]
    ]])
