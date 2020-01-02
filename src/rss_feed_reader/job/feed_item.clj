(ns rss-feed-reader.job.feed_item
  (:require [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.xml :as xml]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.feed_item :as feed-item-mgr]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.date :as dates]))

(def date-formatter (f/formatter "E, dd MMM yyyy HH:mm:ss Z"))

(defn- ->feed-item [feed item]
  {:feed.item.domain/feed        feed
   :feed.item.domain/title       (first (:title item))
   :feed.item.domain/link        (uris/from-string (first (:link item)))
   :feed.item.domain/pub-time    (dates/parse-date (first (:pubDate item))
                                                   date-formatter)
   :feed.item.domain/description (first (:description item))})

(defn- fetch-feed-items [feed]
  (->> (:feed.domain/link feed)
       (str)
       (xml/parse)
       (:content)
       (first)
       (:content)
       (filter (comp #{:item} :tag))
       (map :content)
       (map (fn [item] (reduce #(assoc %1 (:tag %2) (:content %2)) {} item)))
       (map #(->feed-item feed %))))

(defn- fetch-feeds [batch-size]
  (loop [starting-after 0
         result 0]
    (let [feeds (feed-mgr/get-all {:feed.domain/starting-after starting-after
                                   :feed.domain/limit          batch-size})
          feeds-len (count feeds)
          items (->> feeds
                     (map fetch-feed-items)
                     (flatten)
                     (feed-item-mgr/create-multi))
          items-len (count items)
          result (+ result feeds-len)]
      (log/debug items-len "items for" feeds-len "feeds")
      (if (or (empty? feeds) (< feeds-len batch-size))
        result
        (recur (:feed.domain/order-id (last feeds))
               result)))))

(defn run []
  (let [from (tc/to-long (t/now))]
    (log/info "job start")
    (try
      (let [feeds-parsed (fetch-feeds 100)]
        (log/info feeds-parsed "feeds parsed"))

      (catch Exception e
        (log/warn "error while gathering feed items" e))

      (finally
        (let [to (tc/to-long (t/now))]
          (log/info "job elapsed in" (format "%ms" (- to from))))))))
