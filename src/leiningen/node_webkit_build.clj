(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [clojure.pprint :refer [pprint]]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & _]
  (-> (build-app (:node-webkit-build project))
      (dissoc :files)
      (pprint)))
