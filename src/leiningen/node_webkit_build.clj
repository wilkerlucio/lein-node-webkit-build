(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [fs.core :as fs]
            [clojure.java.shell :refer [sh with-sh-dir]]))

(defn create-app-archive [source-path target-path]
  (let [source-path (fs/absolute-path source-path)
        target-path (fs/absolute-path target-path)]
    (with-sh-dir source-path
      (apply sh "zip" "-r" target-path (fs/list-dir source-path))))
  target-path)

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (build-app {:root "/Users/wilkerlucio/Development/sm2/smgui/public"
              :platforms #{:osx :win :linux32 :linux64}
              :osx {:icon "icon-path"}
              :nw-version :latest
              :disable-developer-toolbar true
              :use-lein-project-version true}))
