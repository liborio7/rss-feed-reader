(defproject rss-feed-reader "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example
  .com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [
                 [org.clojure/clojure "1.10.0"]

                 [environ "1.1.0"]
                 [ragtime "0.8.0"]

                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.3"]

                 [ring/ring-core "1.6.3"]
                 [ring/ring-json "0.5.0"]
                 [ring/ring-jetty-adapter "1.6.3"]

                 [metosin/reitit "0.3.10"]

                 [clj-time "0.15.2"]
                 [org.clojure/java.jdbc "0.7.10"]
                 [org.postgresql/postgresql "42.2.2"]
                 [honeysql/honeysql "0.9.8"]
                 ]
  :plugins [[lein-ring "0.12.5"]
            [lein-environ "1.1.0"]]
  :aliases {"migrate"  ["run" "-m" "rss-feed-reader.data.postgres.migrations/migrate"]
            "rollback" ["run" "-m" "rss-feed-reader.data.postgres.migrations/rollback"]}
  :repl-options {:init-ns rss-feed-reader.app}
  :main rss-feed-reader.core
  :profiles {
             :dev {:env          {:environment "dev"}
                   :dependencies [[org.clojure/test.check "0.9.0"]]}
             })
