(ns sparkling.ml.dslapi-test
  (:require [sparkling.conf :as conf]
            [sparkling.api :as s]
            [sparkling.ml.core :as m]
            [sparkling.ml.classification :as cl]
            [sparkling.ml.transform :as xf]
            [clojure.test :as t]
            [sparkling.ml.validation :as v])

  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.sql DataFrame SQLContext]
           [org.apache.spark.ml.classification NaiveBayes LogisticRegression
            DecisionTreeClassifier RandomForestClassifier GBTClassifier ]
           [java.io File]))

;;new story to use the middleware pattern

(def dataset-path (atom ""))

(defn download-dataset
  []
  (let [svm-dataset-path
          "http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary/svmguide1"
          tmpfile (.getPath (File/createTempFile "svmguide" "svm"))
          _ (spit tmpfile (slurp svm-dataset-path))]
    (reset! dataset-path tmpfile)))

(defn dataset-fixture
  [f]
  (download-dataset)
  (f))


(defn add-estimator-pipeline
  "returns a pipeline consisting of a scaler and an estimator"
  [options]
  (let [;scale the features to stay in the 0-1 range
        ss (xf/standard-scaler {:input-col "features"
                                :output-col "nfeatures"})

        ;tell the classifier to look for the modified features
        lr1 (doto (cl/logistic-regression) (.setFeaturesCol "nfeatures"))]
        ;create a pipeline that scales the features first before training
        (m/make-pipeline [ss lr1])))

(defn addregularization
  "sets the regularization parameters to search over"
  [regparam est]
  (v/param-grid [[(.regParam est) (double-array regparam)]]))
(t/use-fixtures :once dataset-fixture)
;(t/run-tests)

 (t/deftest
   defaults

   ;;sensible defaults
   (let [cvhand (-> m/cv-handler
                    (m/add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                    (m/add-estimator cl/logistic-regression)
                    (m/add-evaluator v/binary-classification-evaluator))
         res (first (m/run-pipeline cvhand))]
     (t/is (> res 0.95))))

  (t/deftest with-estimator-options
    ;;add options for the estimator & evaluator
    (let [cvhand (-> m/cv-handler
                     (m/add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                     (m/add-estimator cl/logistic-regression {:elastic-net-param 0.01})
                     (m/add-evaluator v/binary-classification-evaluator {:metric-name "areaUnderPR"} ))
          res (first (m/run-pipeline cvhand))]
      (t/is (> res 0.95))))

 (t/deftest with-handler-options
   ;;add options for the handler
   (let [cvhand (-> (partial m/cv-handler {:num-folds 5})
                    (m/add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                    (m/add-estimator cl/logistic-regression {:elastic-net-param 0.01})
                    (m/add-evaluator v/binary-classification-evaluator {:metric-name "areaUnderPR"} ))
         res (first (m/run-pipeline cvhand))]
     (t/is (> res 0.95))))




(t/deftest train-val-split

  ;defaults for train-test split validator
  (let [tvhand (-> m/tv-handler
                   (m/add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                   (m/add-estimator cl/logistic-regression)
                   (m/add-evaluator v/binary-classification-evaluator))
         res (first (m/run-pipeline tvhand))]
     (t/is (> res 0.95)))

 ;;set the train-test split ratio
  (let [tvhand (-> (partial m/tv-handler {:train-ratio 0.6} )
                   (m/add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                   (m/add-estimator cl/logistic-regression)
                   (m/add-evaluator v/binary-classification-evaluator))
         res (first (m/run-pipeline tvhand))]
     (t/is (> res 0.95)))

  )

(comment
  ;run grid search over regularization parameters provided in the vector
  (let [cvgrid (-> m/cv-handler
                   (m/add-grid-search (partial addregularization [0.1 0.05 0.01]))
                   (m/add-evaluator v/binary-classification-evaluator)
                   (m/add-estimator cl/logistic-regression)
                   (m/add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt")))]
    (m/run-pipeline cvgrid))
  )

(comment
(defn classifier-test-pipeline
  [cls]
  (-> m/cv-handler
      (m/add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt"))
      (m/add-estimator cls)
      (m/add-evaluator v/binary-classification-evaluator)))

;;doesn't work on this dataset as the features are continuous, not binary
(let [ classifiers [cl/logistic-regression
                     #(cl/naive-bayes {:model-type "bernoulli"})]]
      (->> classifiers
           (map classifier-test-pipeline)
           (map run-pipeline))))

(comment
  (let [cvestpipeline
        (-> m/cv-handler
            (m/add-evaluator v/binary-classification-evaluator)
            (m/add-estimator add-estimator-pipeline)
            (m/add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt")))]
    (m/run-pipeline cvestpipeline)))
