(ns node-webkit-build.io
  (:import (java.io File FileOutputStream FileInputStream)
           (org.apache.commons.io.input CountingInputStream)
           (org.apache.commons.io FilenameUtils FileUtils))
  (:require [clj-http.client :as http]
            [me.raynes.fs :as fs]
            [me.raynes.fs.compression :as compression]
            [clojure.java.io :as io]
            [clojure.java.shell :refer [sh with-sh-dir]]
            [node-webkit-build.util :refer [insert-after print-progress-bar]]))

;; aliases
(def reader #'io/reader)
(def file #'io/file)
(def make-parents #'io/make-parents)
(def input-stream #'io/input-stream)
(def output-stream #'io/output-stream)

(def file? #'fs/file?)
(def base-name #'fs/base-name)
(def exists? #'fs/exists?)
(def mkdirs #'fs/mkdirs)

(defn dir-name [path] (.getParent (File. (str path))))

(defn path-join [& parts] (clojure.string/join (File/separator) parts))

(defn path-files [path]
  (->> (file-seq (file path))
       (filter fs/file?)))

(defn archive-base-name [path]
  "Like base name, but aware of multi extensions like .tar.gz"
  (cond
    (re-find #"\.tar\.gz$" path) (base-name path ".tar.gz")
    :else (base-name path true)))

(defn wrap-downloaded-bytes-counter
  "Middleware that provides an CountingInputStream wrapping the stream output"
  [client]
  (fn [req]
    (let [resp (client req)
          counter (CountingInputStream. (:body resp))]
      (merge resp {:body counter
                   :downloaded-bytes-counter counter}))))

(defn download-with-progress [url target]
  (http/with-middleware
    (-> http/default-middleware
        (insert-after http/wrap-redirects wrap-downloaded-bytes-counter)
        (conj http/wrap-lower-case-headers))
    (let [request (http/get url {:as :stream})
          length (Integer. (get-in request [:headers "content-length"] 0))
          buffer-size (* 1024 10)]
      (with-open [input (:body request)
                  output (io/output-stream target)]
        (let [buffer (make-array Byte/TYPE buffer-size)
              counter (:downloaded-bytes-counter request)]
          (loop []
            (let [size (.read input buffer)]
              (when (pos? size)
                (.write output buffer 0 size)
                (print "\r")
                (print-progress-bar (.getByteCount counter) length)
                (recur))))))
      (println))))

(defn copy [input output]
  (sh "cp" "-r" input output))

(defn zip [source-path target-path]
  (let [source-path (fs/absolute-path source-path)
        target-path (fs/absolute-path target-path)]
    (with-sh-dir source-path
      (apply sh "zip" "-r" target-path (fs/list-dir source-path))))
  target-path)

(defn zip-dir [source-path target-path]
  (let [source-path (fs/absolute-path source-path)
        target-path (fs/absolute-path target-path)]
    (with-sh-dir (dir-name source-path)
      (sh "zip" "-r" target-path (base-name source-path)))))

(defn unzip [zip-path target-path]
  (sh "unzip" "-q" zip-path "-d" target-path))

(defn untar-gz [source-path target-path]
  (let [tar-name (FilenameUtils/removeExtension source-path)]
    (compression/gunzip source-path tar-name)
    (compression/untar tar-name target-path)))

(defn extract-file [source-path target-path]
  (let [decompress-fn (cond
                        (re-find #"\.zip$" source-path) unzip
                        (re-find #"\.tar\.gz$" source-path) untar-gz)]
    (decompress-fn source-path target-path)))

(defn relative-path [source target]
  (let [source (fs/absolute-path source)
        target (fs/absolute-path target)]
    (.substring target (-> source count inc))))

(defn copy-ensuring-blank [source target]
  (FileUtils/deleteDirectory (io/file target))
  (make-parents target)
  ;; using FileUtils/copyDirectory corrupts the files preventing the app from launch
  ;; falling back for system copy until figure out the problem
  (copy source target))
