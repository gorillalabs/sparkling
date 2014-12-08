(ns flambo.broadcast
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.broadcast Broadcast]))

(defn broadcast [^JavaSparkContext sc value]
  (.broadcast sc value))

(defn value [^Broadcast broadcast-var]
  (.value broadcast-var))

