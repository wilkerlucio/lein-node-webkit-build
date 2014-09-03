(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & args]
  (-> (build-app {:root                      "/Users/wilkerlucio/Development/sm2/smgui/public"
                  :name                      "Subtitle Master"
                  :platforms                 #{:osx :win :linux32 :linux64}
                  :osx                       {:icon "/Users/wilkerlucio/Dropbox/Design/subtitle master/subtitle_master.icns"}
                  :nw-version                "0.10.3"
                  :disable-developer-toolbar true
                  :use-lein-project-version  true})
      (dissoc :files)
      (pprint)))
