(ns node-webkit-build.io-test
  (:import (org.apache.commons.io FileUtils))
  (:require [clojure.test :refer :all]
            [node-webkit-build.io :refer :all]))

(defn fixture [& paths]
  (apply path-join "test" "fixtures" paths))

(deftest test-path-join
  (is (= "part/other" (path-join "part" "other"))))

(deftest test-extract-file
  (testing "extracting zip files"
    (let [target (fixture "sample-zip")]
      (FileUtils/deleteDirectory (file target))
      (extract-file (fixture "sample.zip") (fixture))
      (is (= [(path-join target "README")] (map str (path-files target))))))
  (testing "extracting tar.gz files"
    (let [target (fixture "sample-tar-gz")]
      (FileUtils/deleteDirectory (file target))
      (extract-file (fixture "sample.tar.gz") (fixture))
      (is (= [(path-join target "README")] (map str (path-files target)))))))

(deftest test-archive-base-name
  (is (= "package" (archive-base-name "path/to/package.zip")))
  (is (= "package" (archive-base-name "path/to/package.tar.gz"))))
