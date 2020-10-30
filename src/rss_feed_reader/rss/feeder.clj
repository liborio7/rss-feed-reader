(ns rss-feed-reader.rss.feeder
  (:require [clojure.tools.logging :as log]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.domain.feed-item :as feed-item]
            [rss-feed-reader.domain.account-feed :as account-feed]
            [rss-feed-reader.rss.parser :as rss]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.time :as time]
            [rss-feed-reader.domain.job.scheduler :as job-scheduler]
            [rss-feed-reader.bot.client :as bot]
            [com.stuartsierra.component :as component])
  (:import (java.time Instant)
           (java.time.temporal ChronoUnit)))

(defn ->feed-item [feed item]
  {:feed.item.domain/feed        feed
   :feed.item.domain/title       (first (:title item))
   :feed.item.domain/link        (uris/from-string (first (:link item)))
   :feed.item.domain/pub-time    (-> (first (:pubDate item))
                                     (time/parse))
   :feed.item.domain/description (first (:description item))})

(defn parse-feed [feed]
  (->> feed
       (:feed.domain/link)
       (rss/parse)
       (flatten)
       (map (partial ->feed-item feed))))

(defn remove-old-feed-items [feed-items]
  (let [threshold (-> (Instant/now)
                      (.minus 2 ChronoUnit/DAYS))
        old (fn [{:feed.item.domain/keys [pub-time]}]
              (.isBefore pub-time threshold))]
    (remove old feed-items)))

(defn remove-existing-feed-items [feeds-items feed-items]
  (let [existing-links (->> feed-items
                            (feed-item/get-by-links feeds-items)
                            (map :feed.item.domain/link))
        existing (fn [feed-item]
                   (some #(= % (:feed.item.domain/link feed-item)) existing-links))]
    (remove existing feed-items)))

(defn publish-feed-items [bot feed-items accounts-feeds-by-feed-id]
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
    (bot/send-message bot chat-id msg))
  (log/trace (count feed-items) "feed(s) item(s) published"))

(defn handler [bot accounts-feeds feeds feeds-items]
  (fn [_payload]
    (log/trace "start feeding")
    (loop [starting-after 0
           result {::feeds-count       0
                   ::feeds-items-count 0}]
      (let [all-feeds (feed/get-all feeds {:starting-after starting-after :limit 10})]
        (if (empty? all-feeds)
          result
          (let [feeds-by-feed-id (->> all-feeds
                                      (mapcat (juxt :feed.domain/id identity))
                                      (apply hash-map))
                accounts-feeds-by-feed-id (->> all-feeds
                                               (map (partial hash-map :account.feed.domain/feed))
                                               (mapcat (partial account-feed/get-by-feed accounts-feeds))
                                               (group-by #(:feed.domain/id (:account.feed.domain/feed %))))
                active-feeds (->> (keys accounts-feeds-by-feed-id)
                                  (select-keys feeds-by-feed-id)
                                  (vals))
                new-feeds-items (->> active-feeds
                                     (mapcat parse-feed)
                                     (remove-old-feed-items)
                                     (remove-existing-feed-items feeds-items)
                                     (feed-item/create-multi! feeds-items))]
            (publish-feed-items bot new-feeds-items accounts-feeds-by-feed-id)
            (recur (:feed.domain/order-id (last all-feeds))
                   (-> result
                       (update ::feeds-count + (count all-feeds))
                       (update ::feeds-items-count + (count new-feeds-items))))))))))

;; component

(defrecord Feeder [config
                   job-scheduler bot accounts-feeds feeds feeds-items
                   job]
  component/Lifecycle
  (start [this]
    (log/info "start feeder")
    (if job
      this
      (let [model {:job.domain/name        "rss-feeder"
                   :job.domain/description "Fetch feed items"}]
        (assoc this :job (job-scheduler/schedule job-scheduler config model (handler bot accounts-feeds feeds feeds-items))))))
  (stop [this]
    (log/info "stop feeder")
    (if job
      (do
        (job-scheduler/cancel job-scheduler job)
        (assoc this :job nil))
      this)))