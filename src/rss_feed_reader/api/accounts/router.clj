(ns rss-feed-reader.api.accounts.router
  (:require [rss-feed-reader.api.accounts.handler :refer :all]))

(def routes
  [
   ["/accounts"
    ["" {:name ::accounts
         :post create-account}]
    ["/:account-id" {:name   ::account
                     :get    get-account
                     :delete delete-account
                     }]
    ["/:account-id/feeds" {:name ::account-feeds
                           :post create-account-feed
                           :get  get-account-feeds
                           }]
    ["/:account-id/feeds/:account-feed-id" {:name   ::account-feed
                                            :get    get-account-feed
                                            :delete delete-account-feed
                                            }]]
   ])