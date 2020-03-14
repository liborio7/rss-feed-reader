(ns rss-feed-reader.rss.feeder
  (:require [clojure.tools.logging :as log]
            [clj-time.format :as f]
            [clojure.xml :as xml]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.feed.item.logic :as feed-item-logic]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.date :as dates]))

(def date-formatter (f/formatter "E, dd MMM yyyy HH:mm:ss Z"))

(defn- ->feed-item [feed item]
  {:feed.item.logic/feed        feed
   :feed.item.logic/title       (first (:title item))
   :feed.item.logic/link        (uris/from-string (first (:link item)))
   :feed.item.logic/pub-time    (dates/parse-date (first (:pubDate item)) date-formatter)
   :feed.item.logic/description (first (:description item))})

(defn filter-existing-feed-items [feed-items]
  (let [existing-links (->> feed-items
                            (feed-item-logic/get-by-links)
                            (map :feed.item.logic/link)
                            (reduce conj []))]
    (let [missing-links (->> feed-items
                             (remove (fn [feed-item]
                                       (some #(= % (:feed.item.logic/link feed-item)) existing-links)
                                       ))
                             (reduce conj []))]
      (log/trace "missing" (count missing-links) "link(s) out of" (count feed-items))
      missing-links)))

(defn- parse [feed]
  (log/trace "parse items for feed" feed)
  (->> (:feed.logic/link feed)
       (str)
       (xml/parse)
       (:content)
       (first)
       (:content)
       (filter (comp #{:item} :tag))
       (map :content)
       (map (fn [item] (reduce #(assoc %1 (:tag %2) (:content %2)) {} item)))
       (map #(->feed-item feed %))))

(defn feed []
  (log/trace "start feeding")
  (loop [starting-after 0
         feeds-cont 0
         new-feeds-items-count 0]
    (let [batch-size 10
          feeds (feed-logic/get-all :starting-after starting-after :limit batch-size)
          feeds-len (count feeds)
          new-feeds-items (apply + (for [feed feeds]
                                     (->> feed
                                          (parse)
                                          (filter-existing-feed-items)
                                          (feed-item-logic/create-multi))))
          new-feeds-items-len (count new-feeds-items)
          feeds-count (+ feeds-cont feeds-len)
          new-feeds-items-count (+ new-feeds-items-count new-feeds-items-len)
          last-feed-order-id (:feed.logic/order-id (last feeds))]
      (log/trace new-feeds-items-len "new feed(s) item(s) created")

      ; get all account feeds by feed id and send feed items to them

      (if (or (empty? feeds) (< feeds-len batch-size))
        {:feed.item.job/feeds-count           feeds-count
         :feed.item.job/new-feeds-items-count new-feeds-items-count}
        (recur last-feed-order-id
               feeds-count
               new-feeds-items-count)))))
