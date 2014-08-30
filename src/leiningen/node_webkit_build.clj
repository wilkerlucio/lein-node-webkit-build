(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [fs.core :as fs]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (let [latest (last (versions-with-data "http://dl.node-webkit.org/"))
        url (get-in latest [:platforms :osx])
        cache-dir "node-webkit-cache"
        output-path (str cache-dir "/" (fs/base-name url))]
    (if (fs/exists? output-path)
      (println "File already exists" output-path)
      (do
        (fs/mkdirs cache-dir)
        (download-with-progress url output-path)))))
