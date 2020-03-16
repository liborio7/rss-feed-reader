(ns rss-feed-reader.telegram.client
  (:require [rss-feed-reader.env :refer [env]]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.spec.alpha :as s]
            [clojure.tools.logging :as log]))

(def url (format "https://api.telegram.org/bot%s" (:telegram-token env)))


;; model

(s/def :telegram.message.from/id int?)
(s/def :telegram.message.from/is-bot boolean?)
(s/def :telegram.message.from/first-name string?)
(s/def :telegram.message.from/username string?)
(s/def :telegram.message.from/language_code string?)

(s/def :telegram.message.chat/id int?)
(s/def :telegram.message.chat/first-name string?)
(s/def :telegram.message.chat/username string?)
(s/def :telegram.message.chat/type string?)

(s/def :telegram.message.entity/offset int?)
(s/def :telegram.message.entity/length int?)
(s/def :telegram.message.entity/type string?)

(s/def :telegram.message/message-id int?)
(s/def :telegram.message/from (s/keys :req [:telegram.message.from/id
                                            :telegram.message.from/is-bot
                                            :telegram.message.from/first-name
                                            :telegram.message.from/username]
                                      :opt [:telegram.message.from/language-code]))
(s/def :telegram.message/from (s/keys :req [:telegram.message.chat/id
                                            :telegram.message.chat/first-name
                                            :telegram.message.chat/username
                                            :telegram.message.chat/type]))
(s/def :telegram.message/date int?)
(s/def :telegram.message/text string?)
(s/def :telegram.message/entities (s/coll-of (s/keys :req [:telegram.message.entity/offset
                                                           :telegram.message.entity/length
                                                           :telegram.message.entity/type])))

;; conversion

(defn- body->model [body]
  (let [body-result (:result (json/parse-string body))
        {:telegram.message/keys []} body-result]
    {}))

(defn- body->models [body]
  (let [body-result (:result (json/parse-string body))]

    ))

;; api

(defn get-updates [offset]
  (log/trace "get updates with offset" offset)
  (http/post (str url "/getUpdates")
             {:query-params {"offset" offset}
              :content-type :json}))

(defn send-message [chat-id text]
  (log/trace "send message to" chat-id ":" text)
  (http/post (str url "/sendMessage")
             {:body         (json/generate-string {:chat_id chat-id
                                                   :text    text})
              :content-type :json}))