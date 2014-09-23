(ns node-webkit-build.core-test
  (:require [clojure.test :refer :all]
            [clojure.data.json :as json]
            [vcr-clj.clj-http :refer [with-cassette]]
            [node-webkit-build.core :refer :all]
            [node-webkit-build.io :as io]
            [slingshot.test]
            [taoensso.timbre :as timbre]))

(def versions-list '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
                     "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2"))

(timbre/set-level! :warn)

(deftest test-read-versions
  (testing "read the version list and add into the request"
    (with-cassette :dl-node-webkit
      (is (= '("0.8.0" "0.8.1" "0.8.2" "0.8.3" "0.8.4" "0.8.5" "0.8.6" "0.8.7"
               "0.9.0" "0.9.1" "0.9.2" "0.9.3" "0.10.0" "0.10.1" "0.10.2")
             (:nw-available-versions (read-versions {})))))))

(deftest test-normalize-version
  (is (= "0.9.1"
         (:nw-version (normalize-version {:nw-version "0.9.1"
                                          :nw-available-versions versions-list}))))
  (is (= "0.10.2"
         (:nw-version (normalize-version {:nw-version :latest
                                          :nw-available-versions versions-list})))))

(deftest test-verify-version
  (is (= {:nw-version "0.9.1"
          :nw-available-versions versions-list}
         (verify-version {:nw-version "0.9.1"
                          :nw-available-versions versions-list})))
  (is (thrown+? [:type ::node-webkit-build.core/invalid-version]
                (verify-version {:nw-version "0.3.4"
                                 :nw-available-versions versions-list}))))

(deftest test-read-package
  (testing "reading a valid package.json file"
    (let [params (read-package {:root "test/fixtures/sample-app"})]
      (is (= {:name   "sample-app"
              :version "0.0.1"}
             (:package params))))))

(deftest test-update-app-name
  (is (= {:name    "New Name"
          :package {:name "New Name"}}
         (update-app-name {:name "New Name"})))
  (is (= {:package {:name "old-name"}}
         (update-app-name {:package {:name "old-name"}}))))

(deftest test-update-app-version
  (is (= {:version "0.1.0"
          :package {:version "0.1.0"}}
         (update-app-version {:version "0.1.0"})))
  (is (= {:package {:version "0.0.0"}}
         (update-app-version {:package {:version "0.0.0"}}))))

(deftest test-read-files
  (testing "reading from a root"
    (let [response (read-files {:root "test/fixtures/sample-app"})]
      (is (= [["package.json" :read]]
             (:files response))))))

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

(deftest test-prepare-package-json
  (testing "it outputs the package contents as json data into package.json"
    (let [package-data {:info "here"}
          res (prepare-package-json {:package package-data
                                     :files [["a" "b"]]})]
      (is (= [["a" "b"]
              ["package.json" (json/write-str package-data)]]
             (:files res))))))

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

(deftest test-osx-read-info-plist
  (let [res (osx-read-info-plist {:contents-path (io/path-join "test" "fixtures")})]
    (is (= {"CFBundleDisplayName"   "node-webkit"
            "CFBundleDocumentTypes" [{"CFBundleTypeIconFile" "nw.icns"
                                      "CFBundleTypeName"     "node-webkit App"
                                      "CFBundleTypeRole"     "Viewer"
                                      "LSHandlerRank"        "Owner"
                                      "LSItemContentTypes"   ["com.intel.nw.app"]}
                                     {"CFBundleTypeName"    "Folder"
                                      "CFBundleTypeOSTypes" ["fold"]
                                      "CFBundleTypeRole"    "Viewer"
                                      "LSHandlerRank"       "None"}]
            "CFBundleExecutable"            "node-webkit"
            "CFBundleIconFile"              "nw.icns"
            "CFBundleIdentifier"            "com.intel.nw"
            "CFBundleInfoDictionaryVersion" "6.0"}
           (:info res))))
  (is (= nil)
      (:info (osx-read-info-plist {}))))

(deftest test-osx-icon
  (let [res-path (io/path-join "tmp" "resources")]
    (io/mkdirs res-path)
    (osx-icon {:resources-path res-path}
              {:osx {:icon (io/path-join "test" "fixtures" "icon.icns")}})
    (is (= "icon contents\n"
           (slurp (io/path-join res-path "nw.icns"))))))

(deftest test-osx-set-plist-name
  (let [res (osx-set-plist-name {:info    {"CFBundleDisplayName" "node-webkit"
                                           "CFBundleIconFile"    "nw.icns"
                                           "CFBundleIdentifier"  "com.intel.nw"}}
                                {:package {:name "App Name"}})]
    (is (= (:info res) {"CFBundleDisplayName" "App Name"
                        "CFBundleIconFile"    "nw.icns"
                        "CFBundleIdentifier"  "com.intel.nw"}))))

(deftest test-osx-export-plist
  (osx-export-plist {:contents-path "tmp"
                     :info {"CFBundleDisplayName" "App Name"}})
  (is (= (slurp "tmp/Info.plist")
         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>
<!DOCTYPE plist PUBLIC \"-//Apple//DTD PLIST 1.0//EN\" \"http://www.apple.com/DTDs/PropertyList-1.0.dtd\">
<plist version=\"1.0\">
  <dict>
    <key>CFBundleDisplayName</key>
    <string>App Name</string>
  </dict>
</plist>
")))
