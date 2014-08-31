(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [clojure.java.io :as io]
            [fs.core :as fs])
  (:import (java.io File)))

(defn archive-path []
  (let [latest (last (versions-with-data "http://dl.node-webkit.org/"))
        url (get-in latest [:platforms :osx])
        cache-dir "node-webkit-cache"
        output-path (str cache-dir (File/separator) (fs/base-name url))]
    (when-not (fs/exists? output-path)
      (io/make-parents output-path)
      (download-with-progress url output-path))
    output-path))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (let [nw-archive (archive-path)]
    (println "Extracting from" nw-archive)))
