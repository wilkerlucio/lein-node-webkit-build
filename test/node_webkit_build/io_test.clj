(ns node-webkit-build.io-test
  (:require [clojure.test :refer :all]
            [node-webkit-build.io :refer :all]))

(deftest test-path-join
  (is (= "part/other" (path-join "part" "other"))))
