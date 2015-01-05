(ns sparkling.api-test
  (:import [org.apache.spark.broadcast Broadcast])
  (:use clojure.test)
  (:require [sparkling.api :as s]
            [sparkling.conf :as conf]
            [sparkling.broadcast :as sb]
            ))




(deftest broadcast
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (s/with-context
      c conf
      (testing
        "gives us a Broadcast Var"
        (is (= (class Broadcast) (sb/broadcast c 'anything))))

      (testing
        "creates a JavaRDD"
        (is (= (sb/value (sb/broadcast c 'anything)) 'anything))))))
