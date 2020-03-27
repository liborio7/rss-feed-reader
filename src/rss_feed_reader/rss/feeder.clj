(ns rss-feed-reader.rss.feeder
  (:require [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [rss-feed-reader.core.account.feed.logic :as account-feed-logic]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.feed.item.logic :as feed-item-logic]
            [rss-feed-reader.telegram.client :as telegram]
            [rss-feed-reader.rss.parser :as rss]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.date :as dates]))

(def date-formatter (f/formatter "E, dd MMM yyyy HH:mm:ss Z"))

(defn- ->feed-item [feed item]
  {:feed.item.logic/feed        feed
   :feed.item.logic/title       (first (:title item))
   :feed.item.logic/link        (uris/from-string (first (:link item)))
   :feed.item.logic/pub-time    (dates/parse-date (first (:pubDate item)) date-formatter)
   :feed.item.logic/description (first (:description item))})

(defn- filter-existing-feed-items [feed-items]
  (let [existing-links (->> feed-items
                            (feed-item-logic/get-by-links)
                            (map :feed.item.logic/link))]
    (let [missing-links (->> feed-items
                             (remove (fn [feed-item]
                                       (some #(= % (:feed.item.logic/link feed-item)) existing-links)
                                       )))]
      (log/trace "missing" (count missing-links) "link(s) out of" (count feed-items))
      missing-links)))

(defn- publish [feeds feed-items]
  (let [accounts-feeds-by-feed-id (->> feeds
                                       (map (partial assoc {} :account.feed.logic/feed))
                                       (map account-feed-logic/get-by-feed)
                                       (flatten)
                                       (group-by #(:feed.logic/id (:account.feed.logic/feed %))))]
    (doseq [feed-item feed-items
            account-feed (->> feed-item
                              (:feed.item.logic/feed)
                              (:feed.logic/id)
                              (get accounts-feeds-by-feed-id))]
      (telegram/send-message (->> account-feed
                                  (:account.feed.logic/account)
                                  (:account.logic/chat-id))
                             (->> feed-item
                                  (:feed.item.logic/link)
                                  (str))))))

(defn feed
  ([] (feed {}))
  ([_]
   (log/trace "start feeding")
   (loop [starting-after 0
          feeds-cont 0
          new-feeds-items-count 0]
     (let [batch-size 10
           feeds (feed-logic/get-all :starting-after starting-after :limit batch-size)
           feeds-len (count feeds)
           new-feeds-items (apply concat
                                  (for [feed feeds]
                                    (->> feed
                                         (:feed.logic/link)
                                         (rss/parse)
                                         (flatten)
                                         (map (partial ->feed-item feed))
                                         (filter-existing-feed-items)
                                         (feed-item-logic/create-multi))))
           new-feeds-items-len (count new-feeds-items)
           feeds-count (+ feeds-cont feeds-len)
           new-feeds-items-count (+ new-feeds-items-count new-feeds-items-len)
           last-feed-order-id (:feed.logic/order-id (last feeds))]
       (publish feeds new-feeds-items)
       (log/trace new-feeds-items-len "new feed(s) item(s) created and published")
       (if (or (empty? feeds) (< feeds-len batch-size))
         {::feeds-count           feeds-count
          ::new-feeds-items-count new-feeds-items-count}
         (recur last-feed-order-id
                feeds-count
                new-feeds-items-count))))))
