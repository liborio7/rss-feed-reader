(ns user
  (:require [rss-feed-reader.env :refer [env]]
            [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh refresh-all]]
            [suspendable.core :as suspendable]
            [rss-feed-reader.bot.client :as bot]
            [rss-feed-reader.domain.account :as account]
            [rss-feed-reader.domain.account-feed :as account-feed]
            [rss-feed-reader.domain.feed :as feed]
            [rss-feed-reader.domain.feed-item :as feed-item]
            [rss-feed-reader.domain.job :as job]
            [rss-feed-reader.web.server :as web-server]
            [rss-feed-reader.domain.job.scheduler :as job-scheduler]
            [rss-feed-reader.rss.feeder :as feeder]
            [rss-feed-reader.bot.server :as bot-server]
            [rss-feed-reader.db.provider.h2 :as datasource]))

(disable-reload!)

(defn system-map []
  {
   ;; db

   :datasource     (datasource/map->H2Datasource {:config {:h2-jdbc "jdbc:h2:./.h2-data/data"}})

   ;; bot

   :bot            (bot/map->TelegramBot {:config (select-keys env [:telegram-token])})

   ;; domain

   :accounts       (component/using
                     (account/map->DbAccounts {})
                     [:datasource])
   :accounts-feeds (component/using
                     (account-feed/map->DbAccountsFeeds {})
                     [:datasource :accounts :feeds])
   :feeds          (component/using
                     (feed/map->DbFeeds {})
                     [:datasource])
   :feeds-items    (component/using
                     (feed-item/map->DbFeedsItems {})
                     [:datasource :feeds])
   :jobs           (component/using
                     (job/map->DbJobs {})
                     [:datasource])

   ;; scheduler

   :job-scheduler  (component/using
                     (job-scheduler/map->JobScheduler {})
                     [:jobs])
   ;:rss-feeder     (component/using
   ;                  (feeder/map->Feeder {:config (:scheduler-rss-feeder env)})
   ;                  [:job-scheduler :bot :accounts-feeds :feeds :feeds-items])
   :bot-server     (component/using
                     (bot-server/map->BotServer {:config (:scheduler-bot-server env)})
                     [:job-scheduler :bot :accounts :accounts-feeds :feeds])

   ;; api

   :web-server     (component/using
                     (web-server/map->WebServer {:config (select-keys env [:port :environment])})
                     [:accounts :accounts-feeds :feeds :feeds-items])
   })

(def system nil)

(def system-initializer (constantly nil))

(defn set-system-initializer! [init]
  (alter-var-root #'system-initializer (constantly init)))

(defn alter-env! [env]
  (alter-var-root #'rss-feed-reader.env/env (partial merge env)))

(defn- stop-system [s]
  (if s (component/stop-system s)))

(defn- init-error []
  (Error. "No system initializer function found."))

(defn init []
  (if-let [init-system #(->> (system-initializer)
                             (merge (system-map))
                             (mapcat identity)
                             (apply component/system-map))]
    (do (alter-var-root #'system #(do (stop-system %) (init-system))) :ok)
    (throw (init-error))))

(defn- try-start-system [start-fn system]
  (try
    (start-fn system)
    (catch Throwable start-ex
      (try
        (component/stop-system (:system (ex-data start-ex)))
        (catch Throwable stop-ex
          (throw (ex-info "System failed during start, also failed to stop failed system"
                          {:start-exception start-ex}
                          stop-ex))))
      (throw start-ex))))

(defn start []
  (alter-var-root #'system #(try-start-system component/start-system %))
  :started)

(defn stop []
  (alter-var-root #'system stop-system)
  :stopped)

(defn go []
  (init)
  (start))

(defn clear []
  (alter-var-root #'system #(do (stop-system %) nil))
  :ok)

(defn suspend []
  (alter-var-root #'system #(if % (suspendable/suspend %)))
  :suspended)

(defn resume []
  (if-let [init system-initializer]
    (do (alter-var-root #'system
                        (fn [system]
                          (try-start-system #(suspendable/resume (init) %)
                                            system)))
        :resumed)
    (throw (init-error))))

(defn reset []
  (suspend)
  (refresh :after 'reloaded.repl/resume))

(defn reset-all []
  (suspend)
  (refresh-all :after 'reloaded.repl/resume))
