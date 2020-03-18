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
(s/def :telegram.message/chat (s/keys :req [:telegram.message.chat/id
                                            :telegram.message.chat/first-name
                                            :telegram.message.chat/username
                                            :telegram.message.chat/type]))
(s/def :telegram.message/date int?)
(s/def :telegram.message/text string?)
(s/def :telegram.message/entities (s/coll-of (s/keys :req [:telegram.message.entity/offset
                                                           :telegram.message.entity/length
                                                           :telegram.message.entity/type])))

(s/def ::message (s/keys :req [:telegram.message/message-id
                               :telegram.message/from
                               :telegram.message/chat
                               :telegram.message/date
                               :telegram.message/text]
                         :opt [:telegram.message/entities]))

(s/def :telegram.update/update-id int?)
(s/def :telegram.update/message ::message)

(s/def ::get-updates (s/coll-of (s/keys :req [:telegram.update/update-id
                                              :telegram.update/message])))

;; conversion

(defn telegram-message-from->model [from]
  (let [{:keys [id is_bot first_name username language_code]} from]
    {:telegram.message.from/id            id
     :telegram.message.from/is-bot        is_bot
     :telegram.message.from/first-name    first_name
     :telegram.message.from/username      username
     :telegram.message.from/language-code language_code}))

(defn telegram-message-chat->model [chat]
  (let [{:keys [id first_name username type]} chat]
    {:telegram.message.chat/id         id
     :telegram.message.chat/first-name first_name
     :telegram.message.chat/username   username
     :telegram.message.chat/type       type}))

(defn telegram-message-entity->model [entity]
  (let [{:keys [offset length type]} entity]
    {:telegram.message.entity/offset offset
     :telegram.message.entity/length length
     :telegram.message.entity/type   type}))

(defn telegram-message->model [message]
  (let [{:keys [message_id from chat date text entities]} message]
    {:telegram.message/message-id message_id
     :telegram.message/from       (telegram-message-from->model from)
     :telegram.message/chat       (telegram-message-chat->model chat)
     :telegram.message/date       date
     :telegram.message/text       text
     :telegram.message/entities   (map telegram-message-entity->model entities)}))

(defn telegram-update->model [update]
  (let [{:keys [update_id message]} update]
    {:telegram.update/update-id update_id
     :telegram.update/message   (telegram-message->model message)}))

;; api

(defn get-updates [offset]
  (log/trace "get updates with offset" offset)
  (let [http-response (http/post (str url "/getUpdates")
                                 {:query-params {"offset" offset}
                                  :content-type :json})
        json-body (:body http-response)]
    (->> (json/parse-string json-body true)
         (:result)
         (map telegram-update->model))))

(defn send-message [chat-id text]
  (log/trace "send message to" chat-id ":" text)
  (let [http-response (http/post (str url "/sendMessage")
                                 {:body         (json/generate-string {:chat_id chat-id
                                                                       :text    text})
                                  :content-type :json})
        json-body (:body http-response)]
    (->> (json/parse-string json-body true)
         (:result)
         (telegram-message->model))))