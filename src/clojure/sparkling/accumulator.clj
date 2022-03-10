(ns sparkling.accumulator
  (:require [sparkling.scalaInterop :as si])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.util AccumulatorV2])
  (:refer-clojure :exclude [name]))

(defn- init-accumulator-to
  [acc value]
  (when-not (zero? value)
    (.add acc value))
  acc)

(defn double-accumulator
  ([^JavaSparkContext sc value]
   (-> (.doubleAccumulator (.sc sc))
       (init-accumulator-to value)))
  ([^JavaSparkContext sc value name]
   (-> (.doubleAccumulator (.sc sc) name)
       (init-accumulator-to value))))

(def accumulator double-accumulator)

(defn long-accumulator
  ([^JavaSparkContext sc value]
   (-> (.longAccumulator (.sc sc))
       (init-accumulator-to value)))
  ([^JavaSparkContext sc value name]
   (-> (.longAccumulator (.sc sc) name)
       (init-accumulator-to value))))

(defn value [^AccumulatorV2 accumulator-var]
  (.value accumulator-var))

(defn name [^AccumulatorV2 accumulator-var]
  (let [name-var (.name accumulator-var)]
    (si/some-or-nil name-var)))
