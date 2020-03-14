(defproject rss-feed-reader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]

                 [environ "1.1.0"]
                 [ragtime "0.8.0"]

                 [org.clojure/tools.logging "0.5.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]

                 [ring/ring-core "1.6.3"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-jetty-adapter "1.6.3"]

                 [metosin/reitit "0.3.10"]

                 [overtone/at-at "1.2.0"]
                 [clj-time "0.15.2"]

                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.2.2"]
                 [honeysql/honeysql "0.9.8"]

                 [clj-http "3.10.0"]
                 [cheshire "5.10.0"]
                 ]
  :plugins [[lein-ring "0.12.5"]
            [lein-environ "1.1.0"]]
  :aliases {"migrate"  ["run" "-m" "rss-feed-reader.db.postgres/migrate"]
            "rollback" ["run" "-m" "rss-feed-reader.db.postgres/rollback"]}
  :repl-options {:init-ns rss-feed-reader.app}
  :main rss-feed-reader.app
  :resource-paths ["resources"]
  :profiles {
             :dev        {:env            {:environment "dev"}
                          :resource-paths ["resources/dev"]
                          :dependencies   [[org.clojure/test.check "0.9.0"]
                                           [orchestra "2018.12.06-2"]]
                          :injections     [(require 'orchestra.spec.test)
                                           (orchestra.spec.test/instrument)]}

             :test       {:env            {:environment "test"}
                          :resource-paths ["resources/test"]
                          :dependencies   [[org.clojure/test.check "0.9.0"]
                                           [orchestra "2018.12.06-2"]]
                          :injections     [(require 'orchestra.spec.test)
                                           (orchestra.spec.test/instrument)]}

             :testing    {:env            {:environment "testing"}
                          :resource-paths ["resources/testing"]}

             :staging    {:env            {:environment "staging"}
                          :resource-paths ["resources/staging"]}

             :production {:env            {:environment "production"}
                          :resource-paths ["resources/production"]}
             })
