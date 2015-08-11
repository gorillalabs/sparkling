(ns sparkling.destructuring
  "Contains wrapper-functions to destructure scala/spark data structures"
  (:refer-clojure :exclude [key first second])
  (:import [scala Tuple2 Tuple3]
           [com.google.common.base Optional]))

(defn tuple [key value]
  (Tuple2. key value))

(defn key [^Tuple2 tuple] (._1 tuple))
(defn value [^Tuple2 tuple] (._2 tuple))
(def first key)
(def second value)


(defn first-value-fn
  "Wraps a function f so that when the wrapped function is called on a tuple2, f is called with the second element from that tuple."
  [f]
  (fn [^Tuple2 tuple]
    (f (._1 tuple))))

(defn second-value-fn
  "Wraps a function f so that when the wrapped function is called on a tuple2, f is called with the second element from that tuple."
  [f]
  (fn [^Tuple2 tuple]
    (f (._2 tuple))))


(def key-fn first-value-fn)
(def value-fn second-value-fn)




(defn key-value-fn
  "wraps a function f [k v] to untuple a key/value tuple. Useful e.g. on map for PairRDD."
  [f]
  (fn [^Tuple2 t]
    (f (._1 t) (._2 t))))

(defn key-seq-seq-fn
  "wraps a function f [k seq1 seq2] to untuple a key/value tuple with two partial values both being seqs. Useful e.g. on map after a cogroup with two RDDs."
  [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple2 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v))))))

(defn seq-seq-fn
  "wraps a function f [seq1 seq2] to untuple a tuple-value with two partial values all being seqs. Useful e.g. on map-values after a cogroup with two RDDs."
  [f]
  (fn [^Tuple2 t]
    (f (seq (._1 t)) (seq (._2 t)))))

(defn key-seq-seq-seq-fn
  "wraps a function f [k seq1 seq2 seq3] to untuple a key/value tuple with three partial values all being seqs. Useful e.g. on map after a cogroup with three RDDs."
  [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple3 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v)) (seq (._3 v))))))

(defn seq-seq-seq-fn
  "wraps a function f [seq1 seq2 seq3] to untuple a triple-value with three partial values all being seqs. Useful e.g. on map-values after a cogroup with three RDDs."
  [f]
  (fn [^Tuple3 v]
    (f (seq (._1 v)) (seq (._2 v)) (seq (._3 v)))))


#_(defn key-val-val-fn

  [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple2 (._2 t)]
      (f k (._1 v) (._2 v)))))

(defn- second-value [^Tuple2 t]
  (._2 t))

(defn- optional-second-value [^Tuple2 t]
  (.orNull ^Optional (._2 t)))

(defn key-val-val-fn
  "wraps a function f [k val1 val2] to untuple a key/value tuple with two partial values. Useful e.g. on map after a join. If optional-second-value? is true (default is false), the second tuple-value might not be there (by use of Optional), e.g. in joins, where the right side might be empty. We'll just call the wrapped function with nil as second value then."
  [f & {:keys [optional-second-value?] :or {optional-second-value? false}}]
  (let [second-value-fn (if optional-second-value?
                          optional-second-value
                          second-value)]
    (fn [^Tuple2 t]
      (let [k (._1 t)
            v ^Tuple2 (._2 t)
            v1 (._1 v)
            v2 (second-value-fn v)]
        (f k v1 v2)))))


(defn val-val-fn
  "wraps a function f [val1 val2] to untuple a value tuple with two partial values. Useful e.g. on map-values after a join. If optional-second-value? is true (default is false), the second tuple-value might not be there (by use of Optional), e.g. in joins, where the right side might be empty. We'll just call the wrapped function with nil as second value then."
  [f & {:keys [optional-second-value?] :or {optional-second-value? false}}]
  (let [second-value-fn (if optional-second-value?
                          optional-second-value
                          second-value)]
    (fn [^Tuple2 v]
      (let [v1 (._1 v)
            v2 (second-value-fn v)]
        (f v1 v2)))))



