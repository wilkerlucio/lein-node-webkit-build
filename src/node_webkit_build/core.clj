(ns node-webkit-build.core
  (:require [clj-http.client :as client]
            [clj-semver.core :as semver]))

(defn version-list [url]
  "Reads an HTTP URL formated as Apache Index looking for semantic version
   numbers, then returns a sorted list of the semantic version numbers found.

   It uses clj-semver to sort the version numbers."
  (->> (client/get url)
       :body
       (re-seq #"v(\d+\.\d+\.\d+)")
       (map second)
       (set)
       (sort semver/cmp)))

(defn version-names [v]
  {:version v
   :platforms {:win (str "v" v "/node-webkit-v" v "-win-ia32.zip")
               :osx (str "v" v "/node-webkit-v" v "-osx-ia32.zip")
               :linux32 (str "v" v "/node-webkit-v" v "-linux-ia32.tar.gz")
               :linux64 (str "v" v "/node-webkit-v" v "-linux-x64.tar.gz")}})
