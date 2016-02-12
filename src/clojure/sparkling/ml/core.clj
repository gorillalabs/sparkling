(ns sparkling.ml.core
  "This is the entry point to the machine learning functionality in Spark"
  (:import [org.apache.spark.api.java JavaSparkContext]
    [org.apache.spark.ml Pipeline PipelineModel PipelineStage]
    [org.apache.spark.sql DataFrame SQLContext ]))

(defn sql-context
  "Returns an SQL context by wrapping a JavaSparkContext object "
  ([^JavaSparkContext sc]
  (SQLContext. sc)))

(defn load-libsvm-dataset
  "Returns a DataFrame object loaded from the location svmfile."
  [^SQLContext sqc svmfile]
  (-> sqc
      (.read)
      (.format "libsvm")
      (.load svmfile)))

(defn make-pipeline
  "Takes a seq consisting of pipeline stages and returns a Pipeline."
  [stages]
  (doto (Pipeline.)
    (.setStages (into-array PipelineStage stages))))
