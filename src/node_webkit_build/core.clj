(ns node-webkit-build.core
  (:import (java.io FileOutputStream FileInputStream)
           (org.apache.commons.io FileUtils IOUtils))
  (:require [clojure.data.json :as json]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :refer [log]]
            [node-webkit-build.versions :as versions]
            [node-webkit-build.io :refer [path-join] :as io]))

(def default-options
  {:platforms #{:osx :win :linux32 :linux64}
   :nw-version :latest
   :output "releases"
   :disable-developer-toolbar true
   :use-lein-project-version true
   :tmp-path (path-join "tmp" "nw-build")})

(defn app-name [req]
  (or (get req :name)
      (get-in req [:package :name])))

(defn read-versions [req]
  (log :info "Reading node-webkit available versions")
  (assoc req :nw-available-versions (versions/versions-list)))

(defn read-package [{:keys [root] :as req}]
  (log :info "Reading package.json")
  (with-open [reader (io/reader (io/file root "package.json"))]
    (let [data (json/read reader :key-fn keyword)]
      (assoc req :package data))))

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

(defn read-files [{:keys [root] :as req}]
  (log :info "Reading files list")
  (let [files (->> (io/path-files root)
                   (map #(io/relative-path root %))
                   (map #(vector % :read))
                   (vec))]
    (assoc req :files files)))

(defn prepare-package-json [{:keys [package] :as req}]
  (update-in req [:files] #(conj % ["package.json" (json/write-str package)])))

(defn disable-developer-toolbar [{:keys [disable-developer-toolbar]
                                  :or {disable-developer-toolbar true}
                                  :as req}]
  (if disable-developer-toolbar
    (assoc-in req [:package :window :toolbar] false)
    req))

(defn ensure-platform [{:keys [platform] :as build} {:keys [tmp-path nw-version]}]
  (let [url (versions/url-for platform nw-version)
        output-path (path-join tmp-path "node-webkit-cache" (io/base-name url))]
    (when-not (io/exists? output-path)
      (log :info (str "Downloading " url))
      (io/make-parents output-path)
      (io/download-with-progress url output-path))
    (assoc build :nw-package output-path)))

(defn output-files [{:keys [files root tmp-path] :as req}]
  (log :info "Writing package files")
  (let [output (get req :tmp-output (path-join tmp-path "app.nw"))]
    (FileUtils/deleteDirectory (io/file output))
    (doseq [[name content] files]
      (let [input-stream (cond
                           (= :read content) (FileInputStream. (io/file root name))
                           (string? content) (IOUtils/toInputStream content)
                           :else (throw+ {:type ::unsupported-input
                                          :input content}))
            file (io/file output name)]
        (io/make-parents file)
        (with-open [out (FileOutputStream. file)
                    in input-stream]
          (IOUtils/copy in out))))
    (assoc req :build-path output)))

(defn unzip-package [{:keys [nw-package] :as build} {:keys [tmp-path]}]
  (io/unzip nw-package tmp-path)
  (assoc build :expanded-nw-package (path-join tmp-path (io/base-name nw-package true))))

(defn create-app-package [build {:keys [tmp-path build-path]}]
  (let [package-path (io/path-join tmp-path "app.nw.zip")]
    (if-not (io/exists? package-path)
      (io/zip build-path package-path))
    (assoc build :app-pack package-path)))

(defn copy-nw-contents [{:keys [platform expanded-nw-package] :as build} {:keys [output] :as req}]
  (let [output (io/file output (name platform) (app-name req))]
    (log :info "Copying" expanded-nw-package "into" output)
    (io/mkdirs output)
    (FileUtils/copyDirectory (io/file expanded-nw-package) output)
    (assoc build :release-path output)))

(defn copy-app-package [{:keys [release-path app-pack] :as build} _]
  (let [pack-target (path-join release-path "app.nw")]
    (log :info "Copying" app-pack "into" pack-target)
    (io/copy app-pack pack-target))
  build)

(defn prepare-simple-build [build req]
  (log :info "Preparing simple build for" build)
  (-> build
      (create-app-package req)
      (copy-nw-contents req)
      (copy-app-package req)))

(defmulti prepare-build (fn [build _] (:platform build)))

(defmethod prepare-build :osx
  [{:keys [expanded-nw-package platform] :as build} {:keys [output build-path] :as req}]
  (let [app-path (path-join expanded-nw-package "node-webkit.app")
        output-path (path-join output (name platform) (str (app-name req) ".app"))
        patch-path (path-join output-path "Contents" "Resources" "app.nw")]
    (FileUtils/deleteDirectory (io/file output-path))
    (log :info (str "Copying " app-path " into " output-path))
    (io/copy app-path output-path)
    (log :info (str "Copying app contents into " patch-path))
    (io/make-parents patch-path)
    (io/copy build-path patch-path)
    build))

(defmethod prepare-build :win
  [build req] (prepare-simple-build build req))

(defn build-platform [req platform]
  (log :info "Building" (name platform))
  (let [build (-> {:platform platform}
                  (ensure-platform req)
                  (unzip-package req)
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
      read-files
      disable-developer-toolbar
      prepare-package-json
      output-files
      build-platforms))
