(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (-> (build-app {:root                      "/Users/wilkerlucio/Development/sm2/smgui/public"
                  :platforms                 #{:osx :win :linux32 :linux64}
                  :osx                       {:icon "icon-path"}
                  :nw-version                :latest
                  :disable-developer-toolbar true
                  :use-lein-project-version  true})
      (dissoc :files)
      (pprint)))
