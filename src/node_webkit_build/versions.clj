(ns node-webkit-build.versions
  (:import (org.apache.commons.io.input CountingInputStream)
           (java.io File FileOutputStream FileInputStream)
           (org.apache.commons.io FileUtils IOUtils))

  (:require [clj-http.client :as http]
            [clj-semver.core :as semver]
            [node-webkit-build.util :refer [map-values]]))

(defn version-list [url]
  "Reads an HTTP URL formated as Apache Index looking for semantic version
   numbers, then returns a sorted list of the semantic version numbers found.

   It uses clj-semver to sort the version numbers."
  (->> (http/get url)
       :body
       (re-seq #"v(\d+\.\d+\.\d+)")
       (map second)
       (set)
       (sort semver/cmp)))

(defn version-names [v]
  {:version v
   :platforms {:win (str "v" v (File/separator) "node-webkit-v" v "-win-ia32.zip")
               :osx (str "v" v (File/separator) "node-webkit-v" v "-osx-ia32.zip")
               :linux32 (str "v" v (File/separator) "node-webkit-v" v "-linux-ia32.tar.gz")
               :linux64 (str "v" v (File/separator) "node-webkit-v" v "-linux-x64.tar.gz")}})

(defn versions-with-data [url]
  (let [full-path (partial str url)
        full-platform-paths (fn [entry] (update-in entry
                                                   [:platforms]
                                                   (partial map-values full-path)))]
    (->> (version-list url)
         (map version-names)
         (map full-platform-paths))))
