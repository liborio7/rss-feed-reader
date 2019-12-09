(ns rss-feed-reader.utils.int)

(defn parse-int [s]
  (try
    (Integer/parseInt s)
    (catch NumberFormatException _
      nil)))
