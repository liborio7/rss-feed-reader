(ns rss-feed-reader.rss.feeder
  (:require [mount.core :refer [defstate]]
            [rss-feed-reader.scheduler.executor :refer [start stop]]
            [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as feeds]
            [rss-feed-reader.domain.feed-item :as feed-items]
            [rss-feed-reader.domain.account-feed :as account-feeds]
            [rss-feed-reader.bot.client :as bot]
            [rss-feed-reader.rss.parser :as rss]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.time :as t])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn ->feed-item [feed item]
  {:feed.item.domain/feed        feed
   :feed.item.domain/title       (first (:title item))
   :feed.item.domain/link        (uris/from-string (first (:link item)))
   :feed.item.domain/pub-time    (-> (first (:pubDate item))
                                     (t/parse))
   :feed.item.domain/description (first (:description item))})

(defn parse-feed [feed]
  (->> feed
       (:feed.domain/link)
       (rss/parse)
       (flatten)
       (map (partial ->feed-item feed))))

(defn filter-old-feed-items [feed-items]
  (let [threshold (-> (Instant/now)
                      (.minus 2 ChronoUnit/DAYS))
        is-old (fn [{:feed.item.domain/keys [pub-time]}]
                 (.isBefore pub-time threshold))]
    (->> feed-items
         (remove is-old))))

(defn filter-existing-feed-items [feed-items]
  (let [existing-links (->> feed-items
                            (feed-items/get-by-links)
                            (map :feed.item.domain/link))]
    (let [missing-links (->> feed-items
                             (remove (fn [feed-item]
                                       (some #(= % (:feed.item.domain/link feed-item)) existing-links)
                                       )))]
      (log/trace "missing" (count missing-links) "link(s) out of" (count feed-items))
      missing-links)))

(defn publish [feed-items accounts-feeds-by-feed-id]
  (doseq [feed-item feed-items
          account-feed (->> feed-item
                            (:feed.item.domain/feed)
                            (:feed.domain/id)
                            (get accounts-feeds-by-feed-id))
          :let [chat-id (->> account-feed
                             (:account.feed.domain/account)
                             (:account.domain/chat-id))
                feed-link (->> feed-item
                               (:feed.item.domain/feed)
                               (:feed.domain/link)
                               (str))
                feed-item-link (->> feed-item
                                    (:feed.item.domain/link)
                                    (str))
                msg (format "From %s:\n\n%s" feed-link feed-item-link)]]
    (bot/send-message chat-id msg)))

(defn feed
  ([] (feed {}))
  ([_]
   (log/trace "start feeding")
   (loop [starting-after 0
          feeds-cont 0
          new-feeds-items-count 0]
     (let [batch-size 10
           feeds (feeds/get-all :starting-after starting-after :limit batch-size)
           feeds-len (count feeds)
           feeds-by-feed-id (->> feeds
                                 (mapcat (juxt :feed.domain/id identity))
                                 (apply hash-map))
           accounts-feeds-by-feed-id (->> feeds
                                          (map (partial hash-map :account.feed.domain/feed))
                                          (mapcat account-feeds/get-by-feed)
                                          (group-by #(:feed.domain/id (:account.feed.domain/feed %))))
           active-feeds (->> (keys accounts-feeds-by-feed-id)
                             (select-keys feeds-by-feed-id)
                             (vals))
           new-feeds-items (->> active-feeds
                                (mapcat parse-feed)
                                (filter-old-feed-items)
                                (filter-existing-feed-items)
                                (feed-items/create-multi!))
           new-feeds-items-len (count new-feeds-items)
           feeds-count (+ feeds-cont feeds-len)
           new-feeds-items-count (+ new-feeds-items-count new-feeds-items-len)
           last-feed-order-id (:feed.domain/order-id (last feeds))]
       (publish new-feeds-items accounts-feeds-by-feed-id)
       (log/trace new-feeds-items-len "new feed(s) item(s) created and published")
       (if (or (empty? feeds) (< feeds-len batch-size))
         {::feeds-count           feeds-count
          ::new-feeds-items-count new-feeds-items-count}
         (recur last-feed-order-id
                feeds-count
                new-feeds-items-count))))))

(defn start-job []
  (let [job-model {:job.domain/name        "rss-feeder"
                   :job.domain/description "Fetch feed items"}]
    (log/info "start feeder job")
    (start :scheduler-rss-feeder job-model feed)))

(defn stop-job [job]
  (log/info "stop feeder job")
  (stop job))

(defstate job
  :start (start-job)
  :stop (stop-job job))


