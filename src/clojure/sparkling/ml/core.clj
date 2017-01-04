(ns sparkling.ml.core
  "This is the entry point to the machine learning functionality in Spark"
  (:require
   [sparkling.conf :as conf]
   [sparkling.core :as s]
   [sparkling.ml.validation :as v])
  (:import [org.apache.spark.api.java JavaSparkContext]
    [org.apache.spark.ml Pipeline PipelineModel PipelineStage]
    [org.apache.spark.sql SQLContext ]))

(defn sql-context
  "Returns an SQL context by wrapping a JavaSparkContext object "
  ([^JavaSparkContext sc]
  (SQLContext. sc)))

(defn load-libsvm-dataset
  "Returns a DataFrame object loaded from the location svmfile."
  [svmfile ^SQLContext sqc ]
  (-> sqc
      (.read)
      (.format "libsvm")
      (.load svmfile)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;  pipeline api
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn make-pipeline
  "Takes a seq consisting of pipeline stages and returns a Pipeline."
  [stages]
  (doto (Pipeline.)
    (.setStages (into-array PipelineStage stages))))

(defn add-dataset
  "load a dataset using dataloadfn, which takes
  the SQLContext as an argument"
  [handler dataloadfn]
  (fn [imap]
    (let[ df (dataloadfn (:sqc imap))]
      (handler (assoc imap :df df)))))

(defn cv-handler
  "returns a list of cross validated metrics"
  ([imap] (cv-handler {} imap))
  ([options imap ]
  (let [cv (v/cross-validator (merge imap options))]
    (v/avg-cv-metrics cv (:df imap)))))

(defn tv-handler
  "trains the data on a split (70%) of the data,
  and returns the metric scored on the remaining 30% of the data"
  ([imap] (tv-handler {} imap))
  ([options imap]
  (let [tv (v/train-val-split-validator (merge imap options))]
    (v/tv-metrics tv (:df imap)))))

(defn add-estimator
  "wraps the handler arguments with an estimator,
  returned by calling make-estimator"
  ([handler make-estimator] (add-estimator handler make-estimator {}))
  ([handler make-estimator est-options]
  (fn [imap]
    (handler (assoc imap :estimator (make-estimator (merge est-options imap)))))))

(defn add-evaluator
  "wraps the handler arguments with an evaluator,
  returned by calling make-evaluator"
  ([handler make-evaluator] (add-evaluator handler make-evaluator {}))
  ([handler make-evaluator eval-options]
   (fn [imap]
     (handler (assoc imap :evaluator (make-evaluator (merge eval-options imap)))))))

(defn add-grid-search
  "middleware that will do a grid search over hyperparameters"
  [handler gsfn]
  (fn [imap]
    (if-let [est (:estimator imap)]
      (handler (assoc imap :estimator-param-maps (gsfn est)))
      (handler imap))))

;;;;;;;;;;;;;;;;;;;;
(defn run-pipeline
  "utility for testing "
  ([pipe] (run-pipeline (-> (conf/spark-conf)
                            (conf/master "local[*]")
                            (conf/app-name "core-test")) pipe))
  ([sconf pipe]
   (s/with-context sc sconf
     (let [sqc (sql-context sc)]
       (pipe {:sqc sqc})))))
