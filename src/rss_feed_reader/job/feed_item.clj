(ns rss-feed-reader.job.feed_item
  (:require [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.xml :as xml]
            [rss-feed-reader.domain.job :as job-mgr]
            [rss-feed-reader.domain.feed :as feed-mgr]
            [rss-feed-reader.domain.feed_item :as feed-item-mgr]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.date :as dates]
            [rss-feed-reader.utils.cid :as cid]))

(def date-formatter (f/formatter "E, dd MMM yyyy HH:mm:ss Z"))

(defn- ->feed-item [feed item]
  {:feed.item.domain/feed        feed
   :feed.item.domain/title       (first (:title item))
   :feed.item.domain/link        (uris/from-string (first (:link item)))
   :feed.item.domain/pub-time    (dates/parse-date (first (:pubDate item)) date-formatter)
   :feed.item.domain/description (first (:description item))})

(defn- fetch-feed-items [feed]
  (log/info "fetch items for feed" feed)
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

(defn filter-existing-feed-items [feed-items]
  (let [existing-links (->> feed-items
                            (feed-item-mgr/get-by-links)
                            (map :feed.item.domain/link)
                            (reduce conj []))]
    (let [missing-links (->> feed-items
                             (remove (fn [feed-item]
                                       (some #(= % (:feed.item.domain/link feed-item)) existing-links)
                                       ))
                             (reduce conj []))]
      (log/info "missing" (count missing-links) "link(s) out of" (count feed-items))
      missing-links)))

(defn- fetch-feeds [batch-size]
  (log/info "fetch feeds with batch size of" batch-size)
  (loop [starting-after 0
         fetched-feeds 0]
    (let [feeds (feed-mgr/get-all :starting-after starting-after :limit batch-size)
          feeds-len (count feeds)
          feeds-items-len (apply + (for [feed feeds]
                                     (->> feed
                                          (fetch-feed-items)
                                          (filter-existing-feed-items)
                                          (feed-item-mgr/create-multi)
                                          (count))))
          fetched-feeds (+ fetched-feeds feeds-len)
          last-feed-order-id (:feed.domain/order-id (last feeds))]
      (log/info feeds-items-len "feeds item(s) created")
      (if (or (empty? feeds) (< feeds-len batch-size))
        {:feed.item.job/feeds-count fetched-feeds}
        (recur last-feed-order-id
               fetched-feeds)))))

(defn run []
  (cid/set-new)
  (log/info "job started")
  (let [job-model {:job.domain/name        "feed-item"
                   :job.domain/description "Fetch feed items"}]
    (try
      (let [job (-> (or (job-mgr/get-by-name job-model) (job-mgr/create job-model))
                    (job-mgr/lock))
            from (tc/to-long (t/now))
            job-result (fetch-feeds 1)
            to (tc/to-long (t/now))
            execution-ms (- to from)]
        (-> (select-keys job [:job.domain/id :job.domain/version])
            (merge {:job.domain/last-execution-ms execution-ms})
            (job-mgr/track_last_execution)
            (job-mgr/unlock))
        (log/info "job elapsed in" (format "%dms" execution-ms) job-result))

      (catch Exception e
        (log/error "error while gathering feed items" e)
        (-> job-model
            (job-mgr/get-by-name)
            (job-mgr/unlock))))))