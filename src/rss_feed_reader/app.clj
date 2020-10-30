(ns rss-feed-reader.app
  (:gen-class)
  (:require [rss-feed-reader.env :refer [env]]
            [rss-feed-reader.utils.cid :as cid]
            [rss-feed-reader.web.server :as web-server]
            [rss-feed-reader.db.provider.postgres :as datasource]
            [rss-feed-reader.bot.client :as bot]
            [com.stuartsierra.component :as component]
            [rss-feed-reader.domain.account :as account]
            [rss-feed-reader.domain.account-feed :as account-feed]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.domain.feed-item :as feed-item]
            [rss-feed-reader.domain.job :as job]
            [rss-feed-reader.domain.job.scheduler :as job-scheduler]
            [rss-feed-reader.domain.feed.item.pruner :as pruner]
            [rss-feed-reader.rss.feeder :as feeder]
            [rss-feed-reader.bot.server :as bot-server]))

(defn filter-config [config prefix]
  (let [with-prefix (fn [[k _]] (clojure.string/starts-with? (name k) prefix))]
    (->> config
         (filter with-prefix)
         (into {}))))

(defn system-map []
  {
   ;; db

   :datasource        (datasource/map->PostgresDatasource {:config (filter-config env "postgres")})

   ;; bot

   :bot               (bot/map->TelegramBot {:config (filter-config env "telegram")})

   ;; domain

   :accounts          (component/using
                        (account/map->DbAccounts {})
                        [:datasource])
   :accounts-feeds    (component/using
                        (account-feed/map->DbAccountsFeeds {})
                        [:datasource :accounts :feeds])
   :feeds             (component/using
                        (feed/map->DbFeeds {})
                        [:datasource])
   :feeds-items       (component/using
                        (feed-item/map->DbFeedsItems {})
                        [:datasource :feeds])
   :jobs              (component/using
                        (job/map->DbJobs {})
                        [:datasource])

   ;; scheduler

   :job-scheduler     (component/using
                        (job-scheduler/map->JobScheduler {})
                        [:jobs])
   :feed-items-pruner (component/using
                        (pruner/map->FeedsItemsPruner {:config (:scheduler-feed-items-pruner env)})
                        [:job-scheduler :feeds-items])
   :rss-feeder        (component/using
                        (feeder/map->Feeder {:config (:scheduler-rss-feeder env)})
                        [:job-scheduler :bot :accounts-feeds :feeds :feeds-items])
   :bot-server        (component/using
                        (bot-server/map->BotServer {:config (:scheduler-bot-server env)})
                        [:job-scheduler :bot :accounts :accounts-feeds :feeds])

   ;; api

   :web-server        (component/using
                        (web-server/map->WebServer {:config (select-keys env [:port])})
                        [:accounts :accounts-feeds :feeds :feeds-items])
   })

(defn -main [& _args]
  (cid/set-new)
  (->> (system-map)
       (mapcat identity)
       (apply component/system-map)
       (component/start-system)))