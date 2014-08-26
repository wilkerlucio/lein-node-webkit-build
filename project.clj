(defproject node-webkit-build "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :eval-in-leiningen true

  :dependencies [[clj-http "1.0.0"]
                 [grimradical/clj-semver "0.2.0"]]

  :profiles {:dev {:dependencies [[org.clojure/data.codec "0.1.0"]
                                  [fs "1.3.3"]]}})
