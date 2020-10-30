(ns rss-feed-reader.system_test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as component]))

(defmacro with-system [[sym system-map] & body]
  `(let [~sym (component/start-system ~system-map)]
     (try
       ~@body
       (finally
         (component/stop-system ~sym)))))

(defmacro with-system-1 [system-map bindings & body]
  `(let [system (component/start-system ~system-map)
         ~@(->> bindings
                (map (comp keyword name keyword))
                (map (fn [k] (list k `system)))
                (interleave bindings))]
     (try
       ~@body
       (finally
         (component/stop-system system)
         ))))