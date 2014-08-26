(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]))

(def server-url "http://dl.node-webkit.org/")

(deftest test-version-list
  (testing "Download the versions list"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7" "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2")
             (version-list server-url))))))

(deftest test-version-names
  (testing "Reading the version names"
    (is (= {:version "0.9.1"
            :platforms {:win "v0.9.1/node-webkit-v0.9.1-win-ia32.zip"
                        :osx "v0.9.1/node-webkit-v0.9.1-osx-ia32.zip"
                        :linux32 "v0.9.1/node-webkit-v0.9.1-linux-ia32.tar.gz"
                        :linux64 "v0.9.1/node-webkit-v0.9.1-linux-x64.tar.gz"}}
           (version-names "0.9.1")))))
