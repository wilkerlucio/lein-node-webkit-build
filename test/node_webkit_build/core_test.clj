(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]))

(deftest listing-versions
  (testing "Download the versions list"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7" "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2")
             (version-list "http://dl.node-webkit.org/"))))))
