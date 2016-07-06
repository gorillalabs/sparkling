(ns sparkling.sql.basic-test
  (:require [clojure.java.io :as io]
            [sparkling.conf :as conf]
            [sparkling.core :as spark]
            [sparkling.sql :as sql]
            [clojure.test :refer :all])
  (:import [org.apache.spark.sql functions]))

(deftest selects-0
    (let [conf (-> (conf/spark-conf)
                   (conf/set "spark.kryo.registrator"
                             "sparkling.testutils.records.registrator.Registrator")
                   (conf/master "local[*]")
                   (conf/app-name "spark sql select columns by name test"))]
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

(deftest select-expr-test
    (let [conf (-> (conf/spark-conf)
                   (conf/set "spark.kryo.registrator"
                             "sparkling.testutils.records.registrator.Registrator")
                   (conf/master "local[*]")
                   (conf/app-name "spark sql select expr test"))]
      (spark/with-context sc conf
        (testing
            (is (= 5
                   (->> sc
                        sql/sql-context
                        (#(sql/read-json % (.getPath (io/resource "euclid/elements.txt"))))
                        (sql/select-expr ["substr(name, 0, 4) as category"])
                        (sql/where "category='C.N.'")
                        .count)))))))

(deftest select-group-agg
    (let [conf (-> (conf/spark-conf)
                   (conf/set "spark.kryo.registrator"
                             "sparkling.testutils.records.registrator.Registrator")
                   (conf/master "local[*]")
                   (conf/app-name "spark sql select expr test"))]
      (spark/with-context sc conf
        (testing
            (is (= "{\"name\":\"Prop.10.93\",\"referenced\":22}"
                   (->> sc
                        sql/sql-context
                        (#(sql/read-json % (.getPath (io/resource "euclid/elements.txt"))))
                        (sql/select-expr ["name" "explode(references) as reference"])
                        (sql/group-by-cols ["name"])
                        (sql/agg ["reference" "count"])
                        (sql/with-column-renamed "count(reference)" "referenced")
                        (sql/order-by [(functions/desc "referenced")])
                        sql/json-rdd spark/first)))))))
