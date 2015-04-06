(ns node-webkit-build.versions
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

(def platform-sufixes
  {:win "win-ia32.zip"
   :osx "osx-ia32.zip"
   :osx64 "osx-x64.zip"
   :linux32 "linux-ia32.tar.gz"
   :linux64 "linux-x64.tar.gz"})

(defn nw-appname [version]
  (if (semver/older? version "0.12.0")
    "node-webkit"
    "nwjs"))

(defn build-prefix [version]
  (if (semver/older? version "0.12.0")
    "node-webkit-v"
    "nwjs-v"))

(defn filename-for [platform version]
  "Returns the file name for a given platform and version."
  (str (build-prefix version) version "-" (get platform-sufixes platform)))

(defn url-for [platform version]
  "Returns the url to download node webkit for a given platform and version."
  (str server-url "v" version "/" (filename-for platform version)))

