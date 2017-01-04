(ns sparkling.ml.validation
  (:import [org.apache.spark.api.java JavaRDD JavaPairRDD JavaSparkContext]
    [org.apache.spark.ml.tuning ParamGridBuilder CrossValidator CrossValidatorModel
     TrainValidationSplit TrainValidationSplitModel]
    [org.apache.spark.ml.param ParamMap]
    [org.apache.spark.mllib.evaluation BinaryClassificationMetrics ]
    [org.apache.spark.mllib.regression LabeledPoint]
    [org.apache.spark.ml Pipeline PipelineModel PipelineStage]
    [org.apache.spark.ml.evaluation BinaryClassificationEvaluator MulticlassClassificationEvaluator]
    [org.apache.spark.sql SQLContext Dataset RowFactory Row Encoders Encoder]))

(defn param-grid
  "Return a ParamMap array, built using ParamGridBuilder .
  Values is a seq where every element is a 2-element vector.
  The first is the parameter name and the second is the list of values "
  ([] (.build (ParamGridBuilder.)))
  ([values]
   (.build (reduce (fn[pgb [k v]] (.addGrid pgb k v) ) (ParamGridBuilder.) values))))

(defn binary-classification-evaluator
  "Returns a BinaryClassificationEvaluator object. The default metric is
  areaUnderROC. The other metric available is 'areaUnderPR' "
  ([] (binary-classification-evaluator {}))
  ([{:keys [metric-name]}]
   (let [bce (BinaryClassificationEvaluator.)]
     (when metric-name (.setMetricName bce metric-name))
     bce)))

(defn multiclass-classification-evaluator
  "Returns a MulticlassClassificationEvaluator object. The default metric is
  f1. The other metrics available are 'precision','recall',
  'weightedPrecision','weightedRecall'"
  ([] (multiclass-classification-evaluator {}))
  ([{:keys [ metric-name]}]
   (let [mce (MulticlassClassificationEvaluator.)]
     (when metric-name (.setMetricName mce metric-name))
     mce)))

(defn avg-cv-metrics
  "Takes 2 arguments, the first is the Crossvalidator,
  the second is the data frame to run CV on.
  Returns a seq of average metrics by training the
  model on cross validated sets of data. The metric is specified
  in the evaluator object"
  [^CrossValidatorModel cv df]
  (seq (.avgMetrics (.fit cv df))))

(defn tv-metrics
  "Takes 2 arguments, the first is the TraintestValidator,
  the second is the data frame to run TV on.
  Returns the validation metrics. The metric is specified
  in the evaluator object"
  [^TrainValidationSplitModel tv df]
  (seq (.validationMetrics (.fit tv df))))

(defn cross-validator
  "Takes a map argument where mandatory keys are :estimator
  (e.g. a classifier like LogisticRegression)
  and :evaluator. (e.g. a BinaryClassificationEvaluator)
  Returns a cross-validator. "
  [{:keys [estimator evaluator
           estimator-param-maps num-folds] :as m}]
  {:pre [(every? m [:estimator :evaluator])]}
  (let [cv (doto (CrossValidator.)
             (.setEstimator estimator)
             (.setEvaluator evaluator)
             (.setEstimatorParamMaps (if estimator-param-maps estimator-param-maps (param-grid))))]
    (when num-folds (.setNumFolds cv num-folds))
    cv))

(defn train-val-split-validator
  "Takes a map argument where mandatory keys are :estimator
  (e.g. a classifier like LogisticRegression)
  and :evaluator. (e.g. a BinaryClassificationEvaluator) .
  Returns a train-validation split object. "
  [{:keys [estimator evaluator
           estimator-param-maps
           train-ratio] :as m}]
  {:pre [(every? m [:estimator :evaluator])]}
  (let [tv (doto (TrainValidationSplit.)
             (.setEstimator estimator)
             (.setEvaluator evaluator)
             (.setEstimatorParamMaps (if estimator-param-maps estimator-param-maps (param-grid))))]
    (when train-ratio (.setTrainRatio tv train-ratio))
    tv))
