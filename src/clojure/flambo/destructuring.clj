(ns flambo.destructuring
  "Contains wrapper-functions to destructure scala/spark data structures"
  (:require [flambo.api :as f])
  (:import [scala Tuple2 Tuple3]))

(f/defsparkfn cogroup-2-fn [f]
              (fn [^Tuple2 t]
                      (let [k (._1 t)
                            v ^Tuple2 (._2 t)]
                        (f k (seq (._1 v)) (seq (._2 v))))))

(f/defsparkfn cogroup-3-fn [f]
              (fn [^Tuple2 t]
                      (let [k (._1 t)
                            v ^Tuple3 (._2 t)]
                        (f k (seq (._1 v)) (seq (._2 v)) (seq (._3 v))))))

(f/defsparkfn tuple-fn [f]
              (fn [^Tuple2 t]
                (f (._1 t) (._2 t))))

(f/defsparkfn tuple-value-fn [f]
              (fn [^Tuple2 t]
                (let [k (._1 t)
                      v ^Tuple2 (._2 t)]
                  (f k (._1 v) (._2 v)))))

(f/defsparkfn second-value [^Tuple2 t]
              (._2 t)
              )

(f/defsparkfn optional-second-value [^Tuple2 t]
              (.orNull (._2 t)))

(f/defsparkfn tuple-value-fn [f & {:keys [optional-second-value?] :or {optional-second-value? false}}]
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