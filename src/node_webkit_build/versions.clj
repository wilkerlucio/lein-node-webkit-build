(ns node-webkit-build.versions
  (:import (org.apache.commons.io.input CountingInputStream)
           (java.io File FileOutputStream FileInputStream)
           (org.apache.commons.io FileUtils IOUtils))
  (:require [clj-http.client :as http]
            [clj-semver.core :as semver]
            [node-webkit-build.util :refer [map-values]]))

(def server-url "http://dl.node-webkit.org/")

(defn versions-list []
  "Reads an HTTP URL formated as Apache Index looking for semantic version
   numbers, then returns a sorted list of the semantic version numbers found.

   It uses clj-semver to sort the version numbers."
  (->> (http/get server-url)
       :body
       (re-seq #"v(\d+\.\d+\.\d+)")
       (map second)
       (set)
       (sort semver/cmp)))

(defn filename-for [platform version]
  "Returns the file name for a given platform and version."
  (let [platform-sufix (condp = platform
                                :win "win-ia32.zip"
                                :osx "osx-ia32.zip"
                                :linux32 "linux-ia32.tar.gz"
                                :linux64 "linux-x64.tar.gz")]
    (str "node-webkit-v" version "-" platform-sufix)))

(defn url-for [platform version]
  "Returns the url to download node webkit for a given platform and version."
  (str server-url "v" version "/" (filename-for platform version)))
