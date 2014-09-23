(ns node-webkit-build.core
  (:import (java.io FileOutputStream FileInputStream)
           (org.apache.commons.io FileUtils IOUtils FilenameUtils))
  (:require [clojure.data.json :as json]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :refer [log]]
            [node-webkit-build.versions :as versions]
            [node-webkit-build.io :refer [path-join] :as io]
            [fs.core :as fs]
            [node-webkit-build.util :as util]
            [com.github.bdesham.clj-plist :refer [parse-plist]]))

(def default-options
  {:platforms #{:osx :osx64 :win :linux32 :linux64}
   :nw-version :latest
   :output "releases"
   :disable-developer-toolbar true
   :use-lein-project-version true
   :tmp-path (path-join "tmp" "nw-build")})

(defn read-versions [req]
  (log :info "Reading node-webkit available versions")
  (assoc req :nw-available-versions (versions/versions-list)))

(defn normalize-version [{:keys [nw-version nw-available-versions] :as req}]
  (if (= nw-version :latest)
    (assoc req :nw-version (last nw-available-versions))
    req))

(defn verify-version [{:keys [nw-version nw-available-versions] :as req}]
  (if ((set nw-available-versions) nw-version)
    req
    (throw+ {:type ::invalid-version
             :message (str "Version " nw-version " is not available.")
             :version nw-version
             :available-versions nw-available-versions})))

(defn read-package [{:keys [root] :as req}]
  (log :info "Reading package.json")
  (with-open [reader (io/reader (io/file root "package.json"))]
    (let [data (json/read reader :key-fn keyword)]
      (assoc req :package data))))

(defn update-app-name [{:keys [name] :as req}]
  (if name
    (assoc-in req [:package :name] name)
    req))

(defn update-app-version [{:keys [version] :as req}]
  (if version
    (assoc-in req [:package :version] version)
    req))

(defn read-files [{:keys [root] :as req}]
  (log :info "Reading files list")
  (let [files (->> (io/path-files root)
                   (map #(io/relative-path root %))
                   (map #(vector % :read))
                   (vec))]
    (assoc req :files files)))

(defn disable-developer-toolbar [{:keys [disable-developer-toolbar]
                                  :or {disable-developer-toolbar true}
                                  :as req}]
  (if disable-developer-toolbar
    (assoc-in req [:package :window :toolbar] false)
    req))

(defn prepare-package-json [{:keys [package] :as req}]
  (update-in req [:files] #(conj % ["package.json" (json/write-str package)])))

(defn output-files [{:keys [files root tmp-path] :as req}]
  (log :info "Writing package files")
  (let [output (get req :tmp-output (path-join tmp-path "app.nw"))]
    (FileUtils/deleteDirectory (io/file output))
    (doseq [[name content] files]
      (with-open [input-stream (cond
                                 (= :read content) (FileInputStream. (io/file root name))
                                 (string? content) (IOUtils/toInputStream content)
                                 :else (throw+ {:type ::unsupported-input
                                                :input content}))]
        (let [file (io/file output name)]
          (io/make-parents file)
          (FileUtils/copyInputStreamToFile input-stream file))))
    (assoc req :build-path output)))

;; Packaging

(defn ensure-platform [{:keys [platform] :as build} {:keys [tmp-path nw-version]}]
  (let [url (versions/url-for platform nw-version)
        output-path (path-join tmp-path "node-webkit-cache" (io/base-name url))]
    (when-not (io/exists? output-path)
      (log :info (str "Downloading " url))
      (io/make-parents output-path)
      (io/download-with-progress url output-path))
    (assoc build :nw-package output-path)))

(defn extract-package [{:keys [nw-package] :as build} {:keys [tmp-path]}]
  (let [target-path (path-join tmp-path (io/archive-base-name nw-package))]
    (if-not (fs/exists? target-path)
      (io/extract-file nw-package tmp-path))
    (assoc build :expanded-nw-package target-path)))

(defn create-app-package [build {:keys [tmp-path build-path]}]
  (let [package-path (io/path-join tmp-path "app.nw.zip")]
    (if-not (io/exists? package-path)
      (io/zip build-path package-path))
    (assoc build :app-pack package-path)))

(defn copy-nw-contents [{:keys [platform expanded-nw-package] :as build} {:keys [output] :as req}]
  (let [output (io/file output (name platform) (get-in req [:package :name]))]
    (log :info "Copying" expanded-nw-package "into" output)
    (io/mkdirs output)
    (FileUtils/copyDirectory (io/file expanded-nw-package) output)
    (assoc build :release-path output)))

(defn merge-app-contents [{:keys [release-path app-pack] :as build} executable]
  (let [pack-target (io/file release-path executable)]
    (log :info "Merging app contents into executable" pack-target)
    (with-open [in (io/input-stream app-pack)
                out (FileOutputStream. pack-target true)]
      (IOUtils/copy in out)))
  build)

(defmulti prepare-build (fn [build _] (:platform build)))

;; OSX Builder

(defn osx-copy-nw-contents [{:keys [expanded-nw-package platform] :as build} {:keys [output] :as req}]
  (let [app-path (path-join expanded-nw-package "node-webkit.app")
        output-path (path-join output (name platform) (str (get-in req [:package :name]) ".app"))]
    (log :info (str "Copying " app-path " into " output-path))
    (io/copy-ensuring-blank app-path output-path)
    (assoc build :release-path output-path)))

(defn osx-inject-app-contents [{:keys [release-path] :as build} {:keys [build-path]}]
  (let [contents-path (path-join release-path "Contents")
        resources-path (path-join contents-path "Resources")
        patch-path (path-join resources-path "app.nw")]
    (log :info (str "Copying app contents into " patch-path))
    (io/copy-ensuring-blank build-path patch-path)
    (merge build {:resources-path resources-path
                  :contents-path contents-path})))

(defn osx-read-info-plist [{:keys [contents-path] :as build}]
  (log :info "Reading Info.plist")
  (let [plist-file (io/file contents-path "Info.plist")]
    (if (io/exists? plist-file)
      (assoc build :info (parse-plist plist-file))
      (do
        (log :info "Can't find Info.list at" plist-file)
        build))))

(defn osx-icon [{:keys [resources-path] :as build} req]
  (when-let [icon (get-in req [:osx :icon])]
    (log :info "Applying OSX icon" icon)
    (io/mkdirs resources-path)
    (FileUtils/copyFile (io/file icon) (io/file resources-path "nw.icns")))
  build)

(defn osx-set-plist-name [build req]
  (let [app-name (get-in req [:package :name])]
    (assoc-in build [:info "CFBundleName"] app-name)))

(defn osx-export-plist [{:keys [contents-path info] :as build}]
  (let [plist-path (path-join contents-path "Info.plist")]
    (log :info "Saving Info.plist file at" plist-path)
    (spit plist-path (util/make-plist-xml-str info))
    build))

(defn osx-build
  [build req]
  (-> build
      (osx-copy-nw-contents req)
      (osx-inject-app-contents req)
      (osx-read-info-plist)
      (osx-set-plist-name req)
      (osx-icon req)
      (osx-export-plist)))

(defmethod prepare-build :osx
  [build req]
  (log :info "Preparing OS X 32 bit build")
  (osx-build build req))

(defmethod prepare-build :osx64
  [build req]
  (log :info "Preparing OS X 64 bit build")
  (osx-build build req))

;; Windows Builder

(defmethod prepare-build :win
  [build req]
  (log :info "Preparing simple build for" build)
  (-> build
      (create-app-package req)
      (copy-nw-contents req)
      (merge-app-contents "nw.exe")))

;; Linux Builder

(defn linux-build [build req]
  (-> build
      (create-app-package req)
      (copy-nw-contents req)
      (merge-app-contents "nw")))

(defmethod prepare-build :linux32
  [build req]
  (log :info "Preparing Linux32 build")
  (linux-build build req))

(defmethod prepare-build :linux64
  [build req]
  (log :info "Preparing Linux64 build")
  (linux-build build req))

(defn build-platform [req platform]
  (log :info "Building" (name platform))
  (let [build (-> {:platform platform}
                  (ensure-platform req)
                  (extract-package req)
                  (prepare-build req))]
    (assoc-in req [:builds platform] build)))

(defn build-platforms [{:keys [platforms] :as req}]
  (reduce build-platform req platforms))

(defn build-app [options]
  (-> (merge default-options options)
      read-versions
      normalize-version
      verify-version
      read-package
      update-app-name
      update-app-version
      read-files
      disable-developer-toolbar
      prepare-package-json
      output-files
      build-platforms))
