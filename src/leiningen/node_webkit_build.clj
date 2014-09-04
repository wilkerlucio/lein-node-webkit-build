(ns leiningen.node-webkit-build
  (:require [node-webkit-build.core :refer :all]
            [clojure.pprint :refer [pprint]]
            [slingshot.slingshot :refer [throw+]]))

(defn node-webkit-build
  "Generates a Node-Webkit build."
  [project & _]
  (let [check-nil (fn [req]
                    (if-not req (throw+ {:type ::nil-configuration
                                         :message "You need to setup :node-webkit-build on your project.clj"})))
        use-lein-version (fn [req]
                           (if (:use-lein-project-version req)
                             (assoc req :version (:version project))
                             req))]
    (build-app (-> (:node-webkit-build project)
                   check-nil
                   use-lein-version))))
