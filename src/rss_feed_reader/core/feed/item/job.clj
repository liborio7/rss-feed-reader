(ns rss-feed-reader.core.feed.item.job
  (:require [clojure.tools.logging :as log]
            [clj-time.coerce :as tc]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clojure.xml :as xml]
            [rss-feed-reader.core.job.logic :as job-logic]
            [rss-feed-reader.core.feed.logic :as feed-logic]
            [rss-feed-reader.core.feed.item.logic :as feed-item-logic]
            [rss-feed-reader.utils.uri :as uris]
            [rss-feed-reader.utils.date :as dates]
            [rss-feed-reader.utils.cid :as cid]))

(def date-formatter (f/formatter "E, dd MMM yyyy HH:mm:ss Z"))

(defn- ->feed-item [feed item]
  {:feed.item.logic/feed        feed
   :feed.item.logic/title       (first (:title item))
   :feed.item.logic/link        (uris/from-string (first (:link item)))
   :feed.item.logic/pub-time    (dates/parse-date (first (:pubDate item)) date-formatter)
   :feed.item.logic/description (first (:description item))})

(defn- fetch-feed-items [feed]
  (log/trace "fetch items for feed" feed)
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
      (log/info "missing" (count missing-links) "link(s) out of" (count feed-items))
      missing-links)))

(defn- fetch-feeds [batch-size]
  (log/trace "fetch feeds with batch size of" batch-size)
  (loop [starting-after 0
         fetched-feeds 0]
    (let [feeds (feed-logic/get-all :starting-after starting-after :limit batch-size)
          feeds-len (count feeds)
          feeds-items-len (apply + (for [feed feeds]
                                     (->> feed
                                          (fetch-feed-items)
                                          (filter-existing-feed-items)
                                          (feed-item-logic/create-multi)
                                          (count))))
          fetched-feeds (+ fetched-feeds feeds-len)
          last-feed-order-id (:feed.logic/order-id (last feeds))]
      (log/trace feeds-items-len "feeds item(s) created")
      (if (or (empty? feeds) (< feeds-len batch-size))
        {:feed.item.job/feeds-count fetched-feeds}
        (recur last-feed-order-id
               fetched-feeds)))))

(defn run []
  (cid/set-new)
  (log/info "job started")
  (let [job-model {:job.logic/name        "feed-item"
                   :job.logic/description "Fetch feed items"}]
    (try
      (let [job (-> (or (job-logic/get-by-name job-model) (job-logic/create job-model))
                    (job-logic/lock))
            from (tc/to-long (t/now))
            job-result (fetch-feeds 1)
            to (tc/to-long (t/now))
            execution-ms (- to from)]
        (-> (select-keys job [:job.logic/id :job.logic/version])
            (merge {:job.logic/last-execution-ms execution-ms})
            (job-logic/track_last_execution)
            (job-logic/unlock))
        (log/info "job elapsed in" (format "%dms" execution-ms) job-result))

      (catch Exception e
        (log/error "error while gathering feed items" e)
        (-> job-model
            (job-logic/get-by-name)
            (job-logic/unlock))))))