(ns sparkling.ml.storyhandler
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

(t/use-fixtures :once dataset-fixture)
;(t/run-tests)

 (t/deftest
   defaults

   ;;sensible defaults
   (let [cvhand (-> cv-handler
                    (add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                    (add-estimator cl/logistic-regression)
                    (add-evaluator v/binary-classification-evaluator))
         res (first (run-pipeline cvhand))]
     (t/is (> res 0.95))))

  (t/deftest with-estimator-options
    ;;add options for the estimator & evaluator
    (let [cvhand (-> cv-handler
                     (add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                     (add-estimator cl/logistic-regression {:elastic-net-param 0.01})
                     (add-evaluator v/binary-classification-evaluator {:metric-name "areaUnderPR"} ))
          res (first (run-pipeline cvhand))]
      (t/is (> res 0.95))))

 (t/deftest with-handler-options
   ;;add options for the handler
   (let [cvhand (-> (partial cv-handler {:num-folds 5})
                    (add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                    (add-estimator cl/logistic-regression {:elastic-net-param 0.01})
                    (add-evaluator v/binary-classification-evaluator {:metric-name "areaUnderPR"} ))
         res (first (run-pipeline cvhand))]
     (t/is (> res 0.95))))




(t/deftest train-val-split

  ;defaults for train-test split validator
  (let [tvhand (-> tv-handler
                   (add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                   (add-estimator cl/logistic-regression)
                   (add-evaluator v/binary-classification-evaluator))
         res (first (run-pipeline tvhand))]
     (t/is (> res 0.95)))

 ;;set the train-test split ratio
  (let [tvhand (-> (partial tv-handler {:train-ratio 0.6} )
                   (add-dataset (partial m/load-libsvm-dataset (deref dataset-path)))
                   (add-estimator cl/logistic-regression)
                   (add-evaluator v/binary-classification-evaluator))
         res (first (run-pipeline tvhand))]
     (t/is (> res 0.95)))

  )

(comment
  ;run grid search over regularization parameters provided in the vector
  (let [cvgrid (-> cv-handler
                   (add-grid-search (partial addregularization [0.1 0.05 0.01]))
                   (add-evaluator v/binary-classification-evaluator)
                   (add-estimator cl/logistic-regression)
                   (add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt")))]
    (run-pipeline cvgrid))
  )

(comment
(defn classifier-test-pipeline
  [cls]
  (-> cv-handler
      (add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt"))
      (add-estimator cls)
      (add-evaluator v/binary-classification-evaluator)))

;;doesn't work on this dataset as the features are continuous, not binary
(let [ classifiers [cl/logistic-regression
                     #(cl/naive-bayes {:model-type "bernoulli"})]]
      (->> classifiers
           (map classifier-test-pipeline)
           (map run-pipeline))))

(comment
  (let [cvestpipeline
        (-> cv-handler
            (add-evaluator v/binary-classification-evaluator)
            (add-estimator add-estimator-pipeline)
            (add-dataset (partial m/load-libsvm-dataset "/tmp/svmguide1.txt")))]
    (run-pipeline cvestpipeline)))
