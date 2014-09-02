(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]
            [slingshot.test]))

(def versions-list '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
                     "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2"))

(deftest test-read-fs-package
  (testing "reading a valid package.json file"
    (let [params (read-package {:root "test/fixtures/sample-app"})]
      (is (= {:name   "sample-app"
              :version "0.0.1"}
             (:package params))))))

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

(deftest test-read-versions
  (testing "read the version list and add into the request"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
               "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2")
             (:nw-available-versions (read-versions {})))))))

(deftest test-normalize-version
  (is (= "0.9.1"
         (:nw-version (normalize-version {:nw-version "0.9.1"
                                          :versions   versions-list}))))
  (is (= "0.10.2"
         (:nw-version (normalize-version {:nw-version :latest
                                          :versions   versions-list})))))

(deftest test-verify-version
  (is (= {:nw-version "0.9.1"
          :versions versions-list}
         (verify-version {:nw-version "0.9.1"
                          :versions versions-list})))
  (is (thrown+? [:type ::node-webkit-build.core/invalid-version]
        (verify-version {:nw-version "0.3.4"
                         :versions versions-list}))))

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
