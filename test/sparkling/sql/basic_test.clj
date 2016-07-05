(ns sparkling.sql.basic-test
  (:require [clojure.java.io :as io]
            [sparkling.conf :as conf]
            [sparkling.core :as spark]
            [sparkling.sql :as sql]
            [clojure.test :refer :all]))

(deftest selects
    (let [conf (-> (conf/spark-conf)
                   (conf/set "spark.kryo.registrator"
                             "sparkling.testutils.records.registrator.Registrator")
                   (conf/master "local[*]")
                   (conf/app-name "hadoop-avro-test"))]
      (spark/with-context sc conf
        (testing
            (is (= "{\"name\":\"C.N.1\",\"description\":\"Things which are equal to the same thing are also equal to one another.\"}"
                   (->> sc
                        sql/sql-context
                        (#(sql/read-json % (.getPath (io/resource "euclid/elements.txt"))))
                        (sql/selects ["name" "description"])
                        (sql/where "name='C.N.1'")
                        sql/json-rdd
                        (spark/take 1)
                        first)))))))
