(ns sparkling.ml.validation-test
  (:require [clojure.test :as t]
            [sparkling.conf :as conf]
            [sparkling.api :as s]
            [sparkling.ml.core :as mlc]
            [clojure.java.io :as io]
            [sparkling.ml.classification :as cl]
            [sparkling.ml.validation :as v])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.sql SQLContext]
           [org.apache.spark.ml.tuning ParamGridBuilder CrossValidator CrossValidatorModel
            TrainValidationSplit TrainValidationSplitModel]
           [org.apache.spark.ml.classification NaiveBayes LogisticRegression
            DecisionTreeClassifier RandomForestClassifier GBTClassifier ]
           [org.apache.spark.ml.evaluation BinaryClassificationEvaluator MulticlassClassificationEvaluator]
           [java.io File]))

(def cconf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "classifier-test")))

(t/deftest validation-test
  (s/with-context c cconf
    (let [sqc (mlc/sql-context c)]

      (t/testing
       "valid classes created "
       (t/is (= (class (v/binary-classification-evaluator)) BinaryClassificationEvaluator))
       (t/is (= (class (v/multiclass-classification-evaluator)) MulticlassClassificationEvaluator)))

      (t/testing
       "mandatory params passed"
       (t/is (thrown? AssertionError (v/cross-validator {})))
       (t/is (thrown? AssertionError (v/cross-validator {:estimator nil})))
       (t/is (thrown? AssertionError (v/train-val-split-validator {})))
       (t/is (thrown? AssertionError (v/train-val-split-validator {:estimator nil}))))

      (t/testing
       "type of validator created "
       (let [estmap  {:estimator (cl/logistic-regression)
                      :evaluator (v/binary-classification-evaluator)}]
         (t/is (= (class (v/cross-validator estmap)) CrossValidator))
         (t/is (= (class (v/train-val-split-validator estmap)) TrainValidationSplit ))))

      (t/testing
       "valid params created "
       (t/is (= (.getMetricName (v/binary-classification-evaluator {:metric-name "areaUnderPR"})) "areaUnderPR" ))
       (t/is (= (.getMetricName (v/multiclass-classification-evaluator {:metric-name "f1"})) "f1" ))))))
