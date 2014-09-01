(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]
            [slingshot.test]))

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

(defn include-properties? [expected m]
  (every? (fn [[k v]] (= v (k m))) expected))

(deftest test-read-fs-package
  (testing "reading a valid package.json file"
    (let [params (read-fs-package {:root "test/fixtures/sample-app"})]
      (is (include-properties?
            {:package {:name    "sample-app"
                       :version "0.0.1"}}
            params)))))

(deftest test-output-files
  (testing "outputs the string contents"
    (output-files {:files [["sample.txt" "sample content"]]
                   :tmp-output "test/fixtures/sample-app-out"})
    (is (= "sample content"
           (slurp "test/fixtures/sample-app-out/sample.txt"))))
  (testing "reads from the root"
    (output-files {:files [["package.json" :read]]
                   :root "test/fixtures/sample-app"
                   :tmp-output "test/fixtures/sample-app-out"})
    (is (= (slurp "test/fixtures/sample-app/package.json")
           (slurp "test/fixtures/sample-app-out/package.json"))))
  (testing "throw error if unsupported input is given"
    (is (thrown+? [:type ::node-webkit-build.core/unsupported-input] (output-files {:files [["sample.txt" :invalid]]
                                                                                    :output "test/fixtures/sample-app-out"})))))

(deftest test-prepare-json
  (testing "it outputs the package contents as json data into package.json"
    (let [package-data {:info "here"}
          res (prepare-package-json {:package package-data
                                     :files [["a" "b"]]})]
      (is (= [["a" "b"]
              ["package.json" (json/write-str package-data)]]
             (:files res))))))

(deftest test-disable-developer-toolbar
  (testing "marks package info to disable toolbar"
    (let [res (disable-developer-toolbar {:disable-developer-toolbar true})]
      (is (= false
             (get-in res [:package :window :toolbar]))))
    (let [res (disable-developer-toolbar {})]
      (is (= false
             (get-in res [:package :window :toolbar]))))
    (let [res (disable-developer-toolbar {:disable-developer-toolbar false})]
      (is (= nil
             (get-in res [:package :window :toolbar]))))))

(deftest test-read-files
  (testing "reading from a root"
    (let [response (read-files {:root "test/fixtures/sample-app"})]
      (is (= [["package.json" :read]]
             (:files response))))))

(deftest test-build-app
  (testing "full integration"
    #_ (build-app {:root "/Users/wilkerlucio/Development/sm2/smgui/public"
                :output "releases/nw-build"
                :platforms #{:osx :win :linux32 :linux64}
                :osx {:icon "icon-path"}
                :disable-developer-toolbar true
                :use-lein-project-version true})))
