(ns sparkling.ml.classification-test
  (:require [clojure.test :as t]
            [sparkling.conf :as conf]
            [sparkling.api :as s]
            [sparkling.ml.core :as mlc]
            [clojure.java.io :as io]
            [sparkling.ml.classification :as cl]
            [sparkling.ml.transform :as xf]
            [sparkling.ml.validation :as v])
  (:import [org.apache.spark.api.java JavaSparkContext]
           [org.apache.spark.sql DataFrame SQLContext]
           [org.apache.spark.ml.classification NaiveBayes LogisticRegression
            DecisionTreeClassifier RandomForestClassifier GBTClassifier ]
           [java.io File]))

(def cconf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "classifier-test")))

(t/deftest cls-test
  (s/with-context c cconf
    (let [sqc (mlc/sql-context c)]

      (t/testing
       "valid classes created "
       (t/is (= (class (cl/logistic-regression)) LogisticRegression))
       (t/is (= (class (cl/naive-bayes)) NaiveBayes)))

    (t/testing
       "valid params passed "
       (t/is (= (.getSmoothing (cl/naive-bayes {:smoothing 0.1})) 0.1 ))))))


(t/deftest classifier-metric
  (s/with-context c cconf
    (let [sqc (mlc/sql-context c)
          svm-dataset-path
          "http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary/svmguide1"
          tmpfile (.getPath (File/createTempFile "svmguide" "svm"))
          _ (spit tmpfile (slurp svm-dataset-path))
          df (mlc/load-libsvm-dataset sqc tmpfile)
          ;create a logistic regression classifier that uses default values
          est (cl/logistic-regression)

          eval-map {:evaluator (v/binary-classification-evaluator)
                    :estimator est}
          ;create a cross validator with the logistic regression classifier
          ;as an estimator. Use a binary evaluator since there are 2 class labels
          cv (v/cross-validator eval-map)

          tv (v/train-val-split-validator eval-map)]


        (t/testing
         "basic usage of cross validation and train-val-split"
         ;get the average validation metrics, should be > 95% (for this dataset)
         (t/is (> (first (v/avg-cv-metrics cv df)) 0.95))
         (t/is (> (first (v/tv-metrics tv df)) 0.95)))

        (t/testing
         "grid search over one param"
         (let [ gsparams (v/param-grid [[(.regParam est) (double-array [0.1 0.05 0.01])]])

                ;;create the cross validator pipeline.
                cv (v/cross-validator (assoc eval-map
                                        ;search for the best choice of regularization parameter
                                        :estimator-param-maps gsparams))]
           (t/is (every? #(> % 0.95) (v/avg-cv-metrics cv df)))))

        (t/testing
         "comparing classifiers "
         (let [;test the performance of logistic regression and Naive Bayes classifiers
               clsa [est (cl/naive-bayes {:model-type "bernoulli"})]

               ;;a function that runs cross validation on the given estimator
               cvfn (fn [cl1] (v/avg-cv-metrics (v/cross-validator (assoc eval-map :estimator cl1)) df))]
           (map cvfn clsa)))


      (t/testing
       "using pipelines "
       (let [;scale the features to stay in the 0-1 range
             ss (xf/standard-scaler {:input-col "features"
                                     :output-col "nfeatures"})

             ;tell the classifier to look for the modified features
             lr1 (doto (cl/logistic-regression) (.setFeaturesCol "nfeatures"))

             ;create a pipeline that scales the features first before training
             mlpipe (mlc/make-pipeline [ss lr1])

             ;validate using cross-validation as usual
             cv (v/cross-validator (assoc eval-map :estimator mlpipe))]
         (t/is (> (first (v/avg-cv-metrics cv df)) 0.95)))))))
