(ns node-webkit-build.util-test
  (:require [clojure.test :refer :all]
            [node-webkit-build.util :refer :all]))

(deftest test-map-values
  (testing "maps the values and keep the keys"
    (is (= {:a 2 :b 4 :c 6}
           (map-values (partial * 2) {:a 1 :b 2 :c 3})))))

(deftest t-make-plist-entry-value
  (is (= (make-plist-entry-value "a")
         {:tag     "string"
          :content ["a"]}))

  (is (= (make-plist-entry-value ["a" "b"])
         {:tag "array"
          :content [{:tag "string"
                     :content ["a"]}
                    {:tag "string"
                     :content ["b"]}]}))

  (is (= (make-plist-entry-value {"a" "b"})
         {:tag "dict"
          :content [{:tag "key"
                     :content ["a"]}
                    {:tag "string"
                     :content ["b"]}]})))

(deftest t-make-plist-entry
  (is (= (make-plist-pairs {"a" "b"})
         [{:tag     "key"
           :content ["a"]}
          {:tag     "string"
           :content ["b"]}]))

  (is (= (make-plist-pairs {:a "b"})
         [{:tag     "key"
           :content ["a"]}
          {:tag     "string"
           :content ["b"]}])))

(deftest t-make-plist
  (is (= {:tag     "plist"
          :attrs   {:version "1.0"}
          :content []}
         (make-plist {})))
  (is (= {:tag     "plist"
          :attrs   {:version "1.0"}
          :content [{:tag     "key"
                     :content ["LSHandlerRank"]}
                    {:tag     "string"
                     :content ["Owner"]}]}
         (make-plist {"LSHandlerRank" "Owner"}))))
