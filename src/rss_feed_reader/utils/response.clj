(ns rss-feed-reader.utils.response)

;; pagination

(defn paginate [seq fn limit]
  {:data     (->> seq
                  (map fn)
                  (take limit))
   :has-more (> (count seq) limit)})

;; 2xx

(defn ok [body]
  {:status 200
   :body   body})

(defn no-content []
  {:status 204})

;; 4xx

(defn bad-request
  ([] (bad-request nil))
  ([body]
   {:status 400
    :body   body}))

(defn not-found []
  {:status  404
   :headers {}})

;; 5xx

(defn server-error
  ([] (server-error nil))
  ([body]
   {:status  500
    :headers {}
    :body    body}))