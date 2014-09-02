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
