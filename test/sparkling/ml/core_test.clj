(ns sparkling.ml.core-test
  (:require [clojure.test :as t]
            [sparkling.conf :as conf]
            [sparkling.api :as s]
            [sparkling.ml.core :as mlc]
            [clojure.java.io :as io])
  (:import [org.apache.spark.api.java JavaSparkContext]
    [org.apache.spark.ml Pipeline PipelineModel PipelineStage]
    [org.apache.spark.sql Dataset SQLContext Column  ]
           [java.io File]))

(t/deftest sql-context
  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "core-test"))]
    (s/with-context c conf
      (let [sqc (mlc/sql-context c)
            svm-dataset-path "data/ml/diabetes"
            df (mlc/load-libsvm-dataset svm-dataset-path sqc )]

        (t/testing
         "creates a SQLContext"
         (t/is (= (class sqc) SQLContext)))

        (t/testing
         "loading libsvm format file, dataset description at
         https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary.html#diabetes"
         (t/is (= (class df) Dataset)))

        (t/testing
         "check number of instances loaded "
         (t/is (= (.count df) 768)))))))
