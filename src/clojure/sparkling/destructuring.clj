(ns sparkling.destructuring
  "Contains wrapper-functions to destructure scala/spark data structures"
  (:refer-clojure :exclude [key first second destructure fn])
  (:import [scala Tuple2 Tuple3]
           [com.google.common.base Optional]))

(defn optional-of [v] (if (nil? v) (Optional/absent) (Optional/of v)))
(defn optional-or-nil [^Optional o] (.orNull o))

(defn tuple [key value]
  (Tuple2. key value))

(defn key [^Tuple2 tuple] (._1 tuple))
(defn value [^Tuple2 tuple] (._2 tuple))
(def first key)
(def second value)


(defn first-value-fn
  "Wraps a function f so that when the wrapped function is called on a tuple2, f is called with the first element from that tuple."
  [f]
  (clojure.core/fn [^Tuple2 tuple]
    (f (._1 tuple))))

(defn second-value-fn
  "Wraps a function f so that when the wrapped function is called on a tuple2, f is called with the second element from that tuple."
  [f]
  (clojure.core/fn [^Tuple2 tuple]
    (f (._2 tuple))))


(def key-fn first-value-fn)
(def value-fn second-value-fn)




(defn key-value-fn
  "wraps a function f [k v] to untuple a key/value tuple. Useful e.g. on map for PairRDD."
  [f]
  (clojure.core/fn [^Tuple2 t]
    (f (._1 t) (._2 t))))

(defn key-seq-seq-fn
  "wraps a function f [k seq1 seq2] to untuple a key/value tuple with two partial values both being seqs. Useful e.g. on map after a cogroup with two RDDs."
  [f]
  (clojure.core/fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple2 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v))))))

(defn seq-seq-fn
  "wraps a function f [seq1 seq2] to untuple a tuple-value with two partial values all being seqs. Useful e.g. on map-values after a cogroup with two RDDs."
  [f]
  (clojure.core/fn [^Tuple2 t]
    (f (seq (._1 t)) (seq (._2 t)))))

(defn key-seq-seq-seq-fn
  "wraps a function f [k seq1 seq2 seq3] to untuple a key/value tuple with three partial values all being seqs. Useful e.g. on map after a cogroup with three RDDs."
  [f]
  (clojure.core/fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple3 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v)) (seq (._3 v))))))

(defn seq-seq-seq-fn
  "wraps a function f [seq1 seq2 seq3] to untuple a triple-value with three partial values all being seqs. Useful e.g. on map-values after a cogroup with three RDDs."
  [f]
  (clojure.core/fn [^Tuple3 v]
    (f (seq (._1 v)) (seq (._2 v)) (seq (._3 v)))))


#_(defn key-val-val-fn

  [f]
  (clojure.core/fn [^Tuple2 t]
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
    (clojure.core/fn [^Tuple2 t]
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
    (clojure.core/fn [^Tuple2 v]
      (let [v1 (._1 v)
            v2 (second-value-fn v)]
        (f v1 v2)))))

;; sparkling destructuring binding

(def tuple-nths '[._1 ._2 ._3 ._4 ._5 ._6 ._7 ._8 ._9 ._10 ._11 ._12 ._13 ._14 ._15 ._16 ._17 ._18 ._19 ._20 ._21 ._22])
(def tuple-classes '[scala.Tuple1 scala.Tuple2 scala.Tuple3 scala.Tuple4 scala.Tuple5 scala.Tuple6 scala.Tuple7 scala.Tuple8 scala.Tuple9 scala.Tuple10 scala.Tuple11 scala.Tuple12 scala.Tuple13 scala.Tuple14 scala.Tuple15 scala.Tuple16 scala.Tuple17 scala.Tuple18 scala.Tuple19 scala.Tuple20 scala.Tuple21 scala.Tuple22])

(declare destructure)

(defn- bind-symbol [parent sym]
  (condp = (clojure.core/first (name sym))
    \- `(~(-> sym name (subs 1) symbol)
         (seq ~parent))
    \? `(~(-> sym name (subs 1) symbol)
         (.orNull ~parent))
    `(~sym ~parent)))

(defn- bind [sym parent form]
  (concat `(~sym ~parent) (destructure sym form)))

(defn- bind-form [parent form]
  (condp #(% %2) form
    symbol?  (bind-symbol parent form)
    seq?     (bind (gensym "seq") parent form)
    vector?  (bind (gensym "vec") parent form)
    map?     (bind (gensym "map") parent form)
    (throw (IllegalArgumentException. (str "Unsupported sparkling tuple-binding form: " form)))))

(defn- destructure [parent form]
  (condp #(% %2) form
    vector? (clojure.core/destructure `[~form ~parent])
    map?    (clojure.core/destructure `[~form ~parent])
    seq?    (apply concat
                   (for [[i subform] (map-indexed vector form)]
                     (bind-form `(~(nth tuple-nths i) ~parent) subform)))
    symbol? (bind-form parent form)))

(defmacro fn [bindings & exprs]
  (let [arg-syms (repeatedly (count bindings) #(gensym "arg"))]
    `(fn* [~@arg-syms]
          (let* [~@(apply concat (map destructure arg-syms bindings))]
            ~@exprs))))

;; TODO: add type-hint metadata to the let* bindings & the Optional
;; TODO: this might be further optimized with protocols
;; http://clj-me.cgrand.net/2010/05/08/destructuring-records-prototypes-and-named-arguments/
