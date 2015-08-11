(ns sparkling.broadcast
  (:require [sparkling.scalaInterop :as si])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.broadcast Broadcast]
           (clojure.lang IDeref)
           ))

(defn broadcast [^JavaSparkContext sc value]
  (let [broadcast-var (.broadcast sc value)]
    (proxy [Broadcast IDeref] [(.id broadcast-var) (si/class-tag (.getClass value))]

      (getValue [] (.value broadcast-var ))

      (doUnpersist [blocking] (.doUnpersist broadcast-var blocking))

      (doDestroy [blocking] (.doDestroy broadcast-var blocking))

      (deref []
        (.value broadcast-var)
        ))))

(defn value [^Broadcast broadcast-var]
  (.getValue broadcast-var))

