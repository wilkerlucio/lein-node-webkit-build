(ns node-webkit-build.util-test
  (:require [clojure.test :refer :all]
            [node-webkit-build.util :refer :all]))

(deftest test-map-values
  (testing "maps the values and keep the keys"
    (is (= {:a 2 :b 4 :c 6}
           (map-values (partial * 2) {:a 1 :b 2 :c 3})))))
