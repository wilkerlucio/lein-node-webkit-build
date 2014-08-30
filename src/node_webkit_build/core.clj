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

(defn tick-by [n]
  (swap! progress/*progress-state* update-in [:done] (partial + n))
  (if-let [f (get progress/*progress-handler* :tick)]
    (f @progress/*progress-state*)))

(defn tick-to [x]
  (swap! progress/*progress-state* assoc-in [:done] x)
  (if-let [f (get progress/*progress-handler* :tick)]
    (f @progress/*progress-state*)))

(defn wrap-downloaded-bytes-counter
  "Middleware that provides an CountingInputStream wrapping the stream output"
  [client]
  (fn [req]
    (let [resp (client req)
          counter (CountingInputStream. (:body resp))]
      (merge resp {:body counter
                   :downloaded-bytes-counter counter}))))

(defn conj-at [v idx val]
  (-> (subvec v 0 idx)
      (conj val)
      (concat (subvec v idx))))

(defn conj-after [v needle val]
  (let [index (.indexOf v needle)]
    (if (neg? index)
      v
      (conj-at v (inc index) val))))

(defn download-with-progress [url target]
  (http/with-middleware
    (-> http/default-middleware
        (conj-after http/wrap-redirects wrap-downloaded-bytes-counter)
        (conj http/wrap-lower-case-headers))
    (let [request (http/get url {:as :stream})
          length (Integer. (get-in request [:headers "content-length"] 0))
          buffer-size (* 1024 10)]
      (progress/init (str "Downloading " url) length)
      (with-open [input (:body request)
                  output (output-stream target)]
        (let [buffer (make-array Byte/TYPE buffer-size)
              counter (:downloaded-bytes-counter request)]
          (loop []
            (let [size (.read input buffer)]
              (when (pos? size)
                (.write output buffer 0 size)
                (tick-to (.getByteCount counter))
                (recur))))))
      (progress/done))))

(defn map-values [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

(defn versions-with-data [url]
  (let [full-path (partial str url)
        full-platform-paths (fn [entry] (update-in entry
                                                   [:platforms]
                                                   (partial map-values full-path)))]
    (->> (version-list url)
         (map version-names)
         (map full-platform-paths))))
