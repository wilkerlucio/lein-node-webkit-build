(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]))

(def server-url "http://dl.node-webkit.org/")

(deftest test-version-list
  (testing "Download the versions list"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
               "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2")
             (version-list server-url))))))

(deftest test-version-names
  (testing "Reading the version names"
    (is (= {:version   "0.9.1"
            :platforms {:win     "v0.9.1/node-webkit-v0.9.1-win-ia32.zip"
                        :osx     "v0.9.1/node-webkit-v0.9.1-osx-ia32.zip"
                        :linux32 "v0.9.1/node-webkit-v0.9.1-linux-ia32.tar.gz"
                        :linux64 "v0.9.1/node-webkit-v0.9.1-linux-x64.tar.gz"}}
           (version-names "0.9.1")))))

(deftest test-map-values
  (testing "maps the values and keep the keys"
    (is (= {:a 2 :b 4 :c 6}
           (map-values (partial * 2) {:a 1 :b 2 :c 3})))))

(deftest test-wrap-stack
  (testing "returns the seed if stack is blank"
    (is (= 0 ((wrap-stack []) 0))))
  (testing "works with a single function"
    (is (= 1 ((wrap-stack [#(fn [req] (inc (% req)))]) 0))))
  (testing "it calls the stack in the correct order"
    (let [mkfn (fn [n] (fn [client]
                         (fn [req]
                           (conj (client (conj req (* n -1))) n))))]
      (is (= [0 -1 -2 -3 3 2 1]
             ((wrap-stack [(mkfn 1) (mkfn 2) (mkfn 3)]) [0]))))))

(defn include-properties? [expected m]
  (every? (fn [[k v]] (= v (k m))) expected))

(deftest test-wrap-read-fs-package
  (testing "reading a valid package.json file"
    (is (include-properties?
          {:package {:name "sample-app"
                     :version "0.0.1"}}
          ((wrap-read-fs-package identity) {:root "test/fixtures/sample-app"})))))

(deftest test-build-app
  (testing "full integration"
    (build-app {:root "/Users/wilkerlucio/Development/sm2/smgui/public"
                :output "releases/nw-build"
                :platforms #{:osx :win :linux32 :linux64}
                :osx {:icon "icon-path"}
                :disable-developer-toolbar true
                :use-lein-project-version true})))
