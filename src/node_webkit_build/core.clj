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
