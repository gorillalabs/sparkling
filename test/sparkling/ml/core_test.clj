(ns sparkling.ml.core-test
  (:require [clojure.test :as t]
            [sparkling.conf :as conf]
            [sparkling.api :as s]
            [sparkling.ml.core :as mlc]
            [clojure.java.io :as io])
  (:import [org.apache.spark.api.java JavaSparkContext]
    [org.apache.spark.ml Pipeline PipelineModel PipelineStage]
    [org.apache.spark.sql DataFrame SQLContext Column  ]
           [java.io File]))

(t/deftest sql-context
  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "core-test"))]
    (s/with-context c conf
      (let [sqc (mlc/sql-context c)
            svm-dataset-path
            "http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary/diabetes"
            tmpfile (.getPath (File/createTempFile "diabetes" "svm"))
            _ (spit tmpfile (slurp svm-dataset-path))
            df (mlc/load-libsvm-dataset tmpfile sqc )]

        (t/testing
         "creates a SQLContext"
         (t/is (= (class sqc) SQLContext)))

        (t/testing
         "loading libsvm format file, dataset description at
         https://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary.html#diabetes"
         (t/is (= (class df) DataFrame)))

        (t/testing
         "check number of instances loaded "
         (t/is (= (.count df) 768)))))))

