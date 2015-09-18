(defproject aggregate "0.1.0-SNAPSHOT"
  :description "A tool to do bulk read/write to a SQL database."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/java.jdbc "0.3.7"]
                 [honeysql "0.6.1"]
                 [java-jdbc/dsl "0.1.3" :scope "test"]
                 [com.h2database/h2 "1.4.188" :scope "test"]]
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
