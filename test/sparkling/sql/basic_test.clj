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
  "test with select-expr, group by, agg, with column renamed, order by and json rdd"
  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator"
                           "sparkling.testutils.records.registrator.Registrator")
                 (conf/master "local[*]")
                 (conf/app-name "spark sql select expr test"))]
    (spark/with-context sc conf
      (testing
          (is (= "{\"reference\":\"Prop.6.1\",\"referenced\":87}"
                 (->> sc
                      sql/sql-context
                      (#(sql/read-json % (.getPath (io/resource "euclid/elements.txt"))))
                      (sql/select-expr ["explode(references) as reference"])
                      (sql/group-by-cols ["reference"])
                      (sql/agg ["reference" "count"])
                      (sql/with-column-renamed "count(reference)" "referenced")
                      (sql/order-by [(functions/desc "referenced")])
                      sql/json-rdd spark/first)))))))

(deftest select-sql
  "test with sql include select, group by, count, join, and register tmp table"
  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator"
                           "sparkling.testutils.records.registrator.Registrator")
                 (conf/master "local[*]")
                 (conf/app-name "spark sql select expr test"))]
    (spark/with-context sc conf
      (testing
          (is (= "{\"name\":\"Prop.10.93\",\"references\":22}"
                 (let [sqlc (sql/sql-context sc)]
                   (->> sqlc
                        (#(sql/read-json % (.getPath (io/resource "euclid/elements.txt"))))
                        (sql/register-temp-table "elements"))
                   (->> sqlc
                        (sql/sql "select name, explode(references) as reference from elements")
                        (sql/group-by-cols ["name"])
                        (sql/agg ["reference" "count"])
                        (sql/with-column-renamed "count(reference)" "references")
                        (sql/register-temp-table "references"))
                   (->> sqlc
                        (sql/sql "select max(references) as count from references")
                        (sql/register-temp-table "element"))
                   (->> sqlc
                        (sql/sql "select references.name, element.count as references
from references join element on references.references = element.count")
                        sql/json-rdd spark/first))))))))

(deftest selects-from-parquet
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
                                      (#(sql/read-parquet % (.getPath (io/resource "parquet/"))))
                                      (sql/selects ["name" "description"])
                                      (sql/where "name='C.N.1'")
                                      sql/json-rdd
                                      (spark/take 1)
                                      first)))))))


