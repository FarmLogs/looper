(defproject com.farmlogs/looper "0.2.0"
  :description "Drop-in clj-http replacement with retries"
  :url "https://github.com/FarmLogs/looper"
  :license {:name "Eclipse Public License"
            :url "https://www.eclipse.org/org/documents/epl-2.0/"}
  :dependencies [[clj-http "3.7.0"]
                 [diehard "0.6.0"]]
  :deploy-repositories [["releases" :clojars]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0"]
                                  [org.clojure/tools.logging "0.4.0"]
                                  [ring/ring-jetty-adapter "1.6.2"]]}})
