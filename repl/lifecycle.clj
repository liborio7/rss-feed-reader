(ns lifecycle
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.namespace.repl :refer [disable-reload! refresh refresh-all]]
            [suspendable.core :as suspendable]))

(disable-reload!)

(def system nil)

(def system-initializer (constantly nil))

(defn set-system-initializer! [init]
  (alter-var-root #'system-initializer (constantly init)))

(defn- stop-system [s]
  (if s (component/stop-system s)))

(defn- init-error []
  (Error. "No system initializer function found."))

(defn init []
  (if-let [init-system #(system-initializer)]
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
