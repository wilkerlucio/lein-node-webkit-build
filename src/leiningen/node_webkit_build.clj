(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [fs.core :as fs]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh with-sh-dir]])
  (:import (java.io File)
           (org.apache.commons.io FileUtils)))

(defn path-join [& parts] (str/join (File/separator) parts))

(defn get-nw-package []
  (let [latest (last (versions-with-data "http://dl.node-webkit.org/"))
        url (get-in latest [:platforms :osx])
        cache-dir "node-webkit-cache"
        output-path (path-join cache-dir (fs/base-name url))]
    (when-not (fs/exists? output-path)
      (io/make-parents output-path)
      (download-with-progress url output-path))
    output-path))

(defn clear-directory [path]
  (FileUtils/deleteDirectory (io/file path))
  (fs/mkdirs path))

(defn create-app-archive [source-path target-path]
  (let [source-path (fs/absolute-path source-path)
        target-path (fs/absolute-path target-path)]
    (with-sh-dir source-path
      (apply sh "zip" "-r" target-path (fs/list-dir source-path))))
  target-path)

(defn unzip [zip-path target-path]
  (sh "unzip" "-q" zip-path "-d" target-path))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (let [release-dir (path-join "releases" "macox")]
    (clear-directory release-dir)
    (let [nw-archive (get-nw-package)]
      (unzip nw-archive release-dir)
      (create-app-archive "/Users/wilkerlucio/Development/sm2/smgui/public"
                          (path-join release-dir "node-webkit-v0.10.2-osx-ia32/node-webkit.app/Contents/Resources" "app.nw"))
      (println "Extracting from" nw-archive))))
