(ns node-webkit-build.util)

(defn map-values [f m]
  (into {} (map (fn [[k v]] [k (f v)]) m)))

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

(declare make-plist-pairs)

(defn make-plist-entry-value [v]
  (cond
    (map? v)
    {:tag "dict"
     :content (make-plist-pairs v)}
    (vector? v)
    {:tag "array"
     :content (mapv make-plist-entry-value v)}
    :else
    {:tag "string"
     :content [v]}))

(defn make-plist-pairs [m]
  (apply concat (for [[k v] m]
                  [{:tag "key"
                    :content [(name k)]}
                   (make-plist-entry-value v)])))

(defn make-plist [m]
  {:tag "plist"
   :attrs {:version "1.0"}
   :content (make-plist-pairs m)})
