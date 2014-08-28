(ns node-webkit-build.core
  (:require [clj-http.client :as http]
            [clj-semver.core :as semver]
            [clj-progress.core :as progress]
            [clojure.java.io :refer [output-stream]])
  (:import (org.apache.commons.io.input CountingInputStream)))

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
   :platforms {:win (str "v" v "/node-webkit-v" v "-win-ia32.zip")
               :osx (str "v" v "/node-webkit-v" v "-osx-ia32.zip")
               :linux32 (str "v" v "/node-webkit-v" v "-linux-ia32.tar.gz")
               :linux64 (str "v" v "/node-webkit-v" v "-linux-x64.tar.gz")}})

(defn tick-n [n]
  (swap! progress/*progress-state* update-in [:done] (partial + n))
  (if-let [f (get progress/*progress-handler* :tick)]
    (f @progress/*progress-state*)))

(defn download-with-progress [url target]
  (http/with-middleware
    (conj http/default-middleware http/wrap-lower-case-headers)
    (let [request (http/get url {:as :stream :decompress-body false})
          length (Integer. (get-in request [:headers "content-length"]))
          buffer-size 1024]
      (progress/init length)
      (with-open [input (->> request
                             :body
                             (CountingInputStream.))
                  output (output-stream target)]
        (let [buffer (make-array Byte/TYPE buffer-size)]
          (loop []
            (let [size (.read input buffer)]
              (when (pos? size)
                (.write output buffer 0 size)
                (tick-n size)
                (recur))))))
      (progress/done))))
