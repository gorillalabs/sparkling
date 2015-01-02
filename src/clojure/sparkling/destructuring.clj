(ns sparkling.destructuring
  "Contains wrapper-functions to destructure scala/spark data structures"
  (:require [sparkling.api :as f])
  (:import [scala Tuple2 Tuple3]
           [com.google.common.base Optional]))

(defn cogroup-2-fn [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple2 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v))))))

(defn cogroup-3-fn [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple3 (._2 t)]
      (f k (seq (._1 v)) (seq (._2 v)) (seq (._3 v))))))

(defn tuple-fn [f]
  (fn [^Tuple2 t]
    (f (._1 t) (._2 t))))

(defn tuple-seq-fn [f]
  (fn [^Tuple2 t]
    (f (seq (._1 t)) (seq (._2 t)))))

(defn tuple-value-fn [f]
  (fn [^Tuple2 t]
    (let [k (._1 t)
          v ^Tuple2 (._2 t)]
      (f k (._1 v) (._2 v)))))

(defn second-value [^Tuple2 t]
  (._2 t)
  )

(defn optional-second-value [^Tuple2 t]
  (.orNull ^Optional (._2 t)))

(defn tuple-value-fn [f & {:keys [optional-second-value?] :or {optional-second-value? false}}]
  (let [second-value-fn (if optional-second-value?
                          optional-second-value
                          second-value)]
    (fn [^Tuple2 t]
      (let [k (._1 t)
            v ^Tuple2 (._2 t)
            v1 (._1 v)
            v2 (second-value-fn v)
            ]
        (f k v1 v2)))))




(defn wrap-fn-after-left-outer-join [f & {:keys [optional-second-value?] :or {optional-second-value? false}}]
  (let [second-value-fn (if optional-second-value?
                          optional-second-value
                          second-value)]
    (fn [^Tuple2 v]
      (let [v1 (._1 v)
            v2 (second-value-fn v)
            ]
        (f v1 v2)))))



(defn wrap-fn-after-cogroup3 [f]
  (fn [^Tuple3 v]
    (f (seq (._1 v)) (seq (._2 v)) (seq (._3 v)))))