(ns node-webkit-build.versions-test
  (:require [clojure.test :refer :all]
            [node-webkit-build.versions :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]))

(deftest test-version-list
  (testing "Download the versions list"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
                "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2"
                "0.10.3" "0.10.4" "0.10.5" "0.11.0" "0.11.1" "0.11.2" "0.11.3"
                "0.11.4" "0.11.5" "0.11.6" "0.12.0")
             (versions-list))))))

(deftest test-filename-for
  (testing "Generating URL for a given platform and version"
    (is (= "node-webkit-v0.9.1-win-ia32.zip"
           (filename-for :win "0.9.1")))
    (is (= "node-webkit-v0.9.1-osx-ia32.zip"
           (filename-for :osx "0.9.1")))
    (is (= "node-webkit-v0.9.1-linux-ia32.tar.gz"
           (filename-for :linux32 "0.9.1")))
    (is (= "node-webkit-v0.9.1-linux-x64.tar.gz"
           (filename-for :linux64 "0.9.1")))))

(deftest test-version-prefix
  (is (= "node-webkit-v" (build-prefix "0.9.1")))
  (is (= "node-webkit-v" (build-prefix "0.11.6")))
  (is (= "nwjs-v" (build-prefix "0.12.0")))
  (is (= "nwjs-v" (build-prefix "0.13.1"))))

(deftest test-url-for
  (testing "Generating URL for a given platform and version"
    (is (= "http://dl.node-webkit.org/v0.9.1/node-webkit-v0.9.1-win-ia32.zip"
           (url-for :win "0.9.1")))
    (is (= "http://dl.node-webkit.org/v0.9.1/node-webkit-v0.9.1-osx-ia32.zip"
           (url-for :osx "0.9.1")))
    (is (= "http://dl.node-webkit.org/v0.9.1/node-webkit-v0.9.1-linux-ia32.tar.gz"
           (url-for :linux32 "0.9.1")))
    (is (= "http://dl.node-webkit.org/v0.9.1/node-webkit-v0.9.1-linux-x64.tar.gz"
           (url-for :linux64 "0.9.1")))
    (is (= "http://dl.node-webkit.org/v0.12.0/nwjs-v0.12.0-osx-x64.zip"
           (url-for :osx64 "0.12.0")))))

(deftest test-for-app-name-by-version
  (testing "The executable names changed when semantic version switched up to 0.12.0"
    (is (= "node-webkit" (nw-appname "0.8.0")))
    (is (= "node-webkit" (nw-appname "0.11.6")))
    (is (= "nwjs" (nw-appname "0.12.0")))))
