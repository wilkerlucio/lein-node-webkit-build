(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [fs.core :as fs]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (let [latest (last (versions-with-data "http://dl.node-webkit.org/"))
        url (get-in latest [:platforms :osx])
        cache-path "node-webkit-cache"]
    (fs/mkdirs cache-path)
    (download-with-progress url (str cache-path "/" (fs/base-name url)))))
