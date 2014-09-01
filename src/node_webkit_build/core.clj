(ns node-webkit-build.core
  (:import (org.apache.commons.io.input CountingInputStream)
           (java.io File FileOutputStream FileInputStream)
           (org.apache.commons.io FileUtils IOUtils))
  (:require [clj-http.client :as http]
            [clj-semver.core :as semver]
            [fs.core :as fs]
            [clojure.data.json :as json]
            [clojure.java.io :as io :refer [output-stream]]
            [slingshot.slingshot :refer [throw+]]
            [taoensso.timbre :refer [log]]
            [clojure.java.shell :refer [sh with-sh-dir]]))


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
   :platforms {:win (str "v" v (File/separator) "node-webkit-v" v "-win-ia32.zip")
               :osx (str "v" v (File/separator) "node-webkit-v" v "-osx-ia32.zip")
               :linux32 (str "v" v (File/separator) "node-webkit-v" v "-linux-ia32.tar.gz")
               :linux64 (str "v" v (File/separator) "node-webkit-v" v "-linux-x64.tar.gz")}})

(defn wrap-downloaded-bytes-counter
  "Middleware that provides an CountingInputStream wrapping the stream output"
  [client]
  (fn [req]
    (let [resp (client req)
          counter (CountingInputStream. (:body resp))]
      (merge resp {:body counter
                   :downloaded-bytes-counter counter}))))

(defn insert-at [v idx val]
  (-> (subvec v 0 idx)
      (conj val)
      (concat (subvec v idx))))

(defn insert-after [v needle val]
  (let [index (.indexOf v needle)]
    (if (neg? index)
      v
      (insert-at v (inc index) val))))

(defn print-progress-bar
  "Render a simple progress bar given the progress and total. If the total is zero
   the progress will run as indeterminated."
  ([progress total] (print-progress-bar progress total {}))
  ([progress total {:keys [bar-width]
                    :or   {bar-width 50}}]
    (if (pos? total)
      (let [pct (/ progress total)
            render-bar (fn []
                         (let [bars (Math/floor (* pct bar-width))
                               pad (- bar-width bars)]
                           (str (clojure.string/join (repeat bars "="))
                                (clojure.string/join (repeat pad " ")))))]
        (print (str "[" (render-bar) "] "
                    (int (* pct 100)) "% "
                    progress "/" total)))
      (let [render-bar (fn [] (clojure.string/join (repeat bar-width "-")))]
        (print (str "[" (render-bar) "] "
                    progress "/?"))))))

(defn download-with-progress [url target]
  (http/with-middleware
    (-> http/default-middleware
        (insert-after http/wrap-redirects wrap-downloaded-bytes-counter)
        (conj http/wrap-lower-case-headers))
    (let [request (http/get url {:as :stream})
          length (Integer. (get-in request [:headers "content-length"] 0))
          buffer-size (* 1024 10)]
      (with-open [input (:body request)
                  output (output-stream target)]
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

(defn read-fs-package [{:keys [root] :as req}]
  (with-open [reader (io/reader (io/file root "package.json"))]
    (let [data (json/read reader :key-fn keyword)]
      (assoc req :package data))))

(defn path-join [& parts] (clojure.string/join (File/separator) parts))

(defn path-files [path]
  (->> (fs/walk (fn [root _ files]
                  (map #(path-join (.toString root) %) files)) path)
       (flatten)
       (set)
       (filter fs/file?)))

(defn relative-path [source target]
  (let [source (fs/absolute-path source)
        target (fs/absolute-path target)]
    (.substring target (-> source count inc))))

(defn read-files [{:keys [root] :as req}]
  (let [files (->> (path-files root)
                   (map #(relative-path root %))
                   (map #(vector % :read))
                   (vec))]
    (assoc req :files files)))

(defn prepare-package-json [{:keys [package] :as req}]
  (update-in req [:files] #(conj % ["package.json" (json/write-str package)])))

(defn disable-developer-toolbar [{:keys [disable-developer-toolbar]
                                  :or {disable-developer-toolbar true}
                                  :as req}]
  (if disable-developer-toolbar
    (assoc-in req [:package :window :toolbar] false)
    req))

(defn ensure-platform [{:keys [tmp-path platform] :as req}]
  (let [latest (last (versions-with-data "http://dl.node-webkit.org/"))
        url (get-in latest [:platforms platform])
        output-path (path-join tmp-path "node-webkit-cache" (fs/base-name url))]
    (when-not (fs/exists? output-path)
      (log :info (str "Downloading " url))
      (io/make-parents output-path)
      (download-with-progress url output-path))
    (assoc req :nw-package output-path)))

(defn output-files [{:keys [files root tmp-path] :as req}]
  (let [output (get req :tmp-output (path-join tmp-path "app.nw"))]
    (FileUtils/deleteDirectory (io/file output))
    (doseq [[name content] files]
      (let [input-stream (cond
                           (= :read content) (FileInputStream. (io/file root name))
                           (string? content) (IOUtils/toInputStream content)
                           :else (throw+ {:type ::unsupported-input}))
            file (io/file output name)]
        (io/make-parents file)
        (with-open [out (FileOutputStream. file)
                    in input-stream]
          (IOUtils/copy in out))))
    (assoc req :build-path output)))

(defn unzip [zip-path target-path]
  (sh "unzip" "-q" zip-path "-d" target-path))

(defn unzip-package [{:keys [nw-package tmp-path] :as req}]
  (unzip nw-package tmp-path)
  (assoc req :expanded-nw-package (path-join tmp-path (fs/base-name nw-package true))))

(defn debug-key [req key]
  (log :info (key req))
  req)

(defn prepare-osx-build [{:keys [output expanded-nw-package platform build-path] :as req}]
  (let [app-path (path-join expanded-nw-package "node-webkit.app")
        output-path (path-join output (name platform) (str (get-in req [:package :name]) ".app"))
        patch-path (path-join output-path "Contents" "Resources" "app.nw")]
    (log :info (str "Copying " app-path " into " output-path))
    (sh "cp" "-r" app-path output-path)
    (log :info (str "Copying app contents into " patch-path))
    (io/make-parents patch-path)
    (sh "cp" "-r" build-path patch-path)
    req))

(defn build-osx [req]
  (when ((get req :platforms #{}) :osx)
    (log :info "Building OSX")
    (-> (assoc req :platform :osx)
        (ensure-platform)
        (unzip-package)
        (debug-key :expanded-nw-package)
        (prepare-osx-build)))
  req)

(def default-options
  {:platforms #{:osx :win :linux32 :linux64}
   :nw-version :latest
   :output "releases"
   :disable-developer-toolbar true
   :use-lein-project-version true
   :tmp-path (path-join "tmp" "nw-build")})

(defn build-app [options]
  (let [with-log (fn [req info f]
                   (log :info info)
                   (f req))]
    (-> (merge default-options options)
        (with-log "Reading package.json" read-fs-package)
        (with-log "Reading root list" read-files)
        disable-developer-toolbar
        (with-log "Building package.json" prepare-package-json)
        (with-log "Writing output files" output-files)
        build-osx)))
