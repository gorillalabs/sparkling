(ns sparkling.accumulator
  (:require [sparkling.scalaInterop :as si])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark Accumulator]
           )
  (:refer-clojure :exclude [name]))

(defn accumulator
  ([^JavaSparkContext sc value]
   (.accumulator sc value))
  ([^JavaSparkContext sc value name]
   (.accumulator sc value name)))

(defn value [^Accumulator accumulator-var]
  (.value accumulator-var))

(defn name [^Accumulator accumulator-var]
  (let [name-var (.name accumulator-var)]
    (si/some-or-nil name-var)))
