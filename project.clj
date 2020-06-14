(defproject rss-feed-reader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]
                 [clj-kondo "RELEASE"]
                 [clj-time "0.15.2"]

                 ; env
                 [environ "1.1.0"]

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
            "midje"    ["with-profile" "test" "midje"]
            "dev"      ["with-profile" "dev" "run" "-m" "rss-feed-reader.app"]
            "migrate"  ["run" "-m" "rss-feed-reader.db.postgres/migrate"]
            "rollback" ["run" "-m" "rss-feed-reader.db.postgres/rollback"]}
  :repl-options {:init-ns rss-feed-reader.app}
  :target-path "target/%s"
  :resource-paths ["resources"]
  :profiles {
             :project/instrument {:dependencies [[org.clojure/test.check "0.9.0"]
                                                 [orchestra "2018.12.06-2"]]
                                  :injections   [(require 'orchestra.spec.test)
                                                 (orchestra.spec.test/instrument)]}

             :project/dev        {:source-paths ["dev"]
                                  :jvm-opts     ["-Dport=3000"]}

             :project/test       {:dependencies [[midje "1.9.9"]]
                                  :plugins      [[lein-midje "3.2.1"]
                                                 [lein-eftest "0.5.9"]]}

             :repl               [:project/instrument
                                  :project/dev
                                  :project/test
                                  {:env            {:environment "repl"}
                                   :resource-paths ["resources/repl"]}]

             :dev                [:project/instrument
                                  :project/dev
                                  :project/test
                                  {:env            {:environment "dev"}
                                   :resource-paths ["resources/dev"]}]

             :test               [:project/instrument
                                  :project/test
                                  {:env            {:environment "test"}
                                   :resource-paths ["resources/test"]}]

             :release            {:resource-paths ["resources/release"]}

             :uberjar            [:release]
             })
