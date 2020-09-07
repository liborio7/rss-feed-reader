(defproject rss-feed-reader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [clj-kondo "RELEASE"]
                 [clojure.java-time "0.3.2"]

                 ; env
                 [environ "1.1.0"]

                 ; di
                 [mount "0.1.16"]

                 ; logging
                 [org.clojure/tools.logging "0.5.0"]
                 [ch.qos.logback/logback-classic "1.2.3"]

                 ; web server & routing
                 [ring/ring-core "1.6.3"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-jetty-adapter "1.6.3"]
                 [ring-cors "0.1.13"]
                 [metosin/reitit "0.3.10"]

                 ; db & migration
                 [hikari-cp "2.10.0"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.2.2"]
                 [honeysql/honeysql "0.9.8"]
                 [ragtime "0.8.0"]

                 ; scheduler
                 [overtone/at-at "1.2.0"]

                 ; http & json
                 [clj-http "3.10.0"]
                 [cheshire "5.10.0"]
                 ]
  :jvm-opts ["-Xms256M" "-Xmx256M"]
  :plugins [[lein-ring "0.12.5"]
            [lein-environ "1.1.0"]]
  :aliases {"kondo"    ["run" "-m" "clj-kondo.main" "--lint" "src"]
            "kaocha"   ["run" "-m" "kaocha.runner"]
            "migrate"  ["run" "-m" "rss-feed-reader.db.datasource/migrate"]
            "rollback" ["run" "-m" "rss-feed-reader.db.datasource/rollback"]}
  :main rss-feed-reader.app
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {
             :project/instrument {:dependencies [[org.clojure/test.check "0.9.0"]
                                                 [orchestra "2018.12.06-2"]]
                                  :injections   [(require 'orchestra.spec.test)
                                                 (orchestra.spec.test/instrument)]}

             :project/repl       {:dependencies   [[org.clojure/tools.namespace "1.0.0"]]
                                  :source-paths   ["dev"]
                                  :resource-paths ["resources/dev"]}

             :project/test       {:dependencies [[lambdaisland/kaocha "1.0.672"]]}

             :dev                [:project/instrument
                                  :project/repl
                                  {:env {:environment "dev"}}]

             :test               [:project/instrument
                                  :project/test
                                  {:env {:environment "test"}}]
             })
