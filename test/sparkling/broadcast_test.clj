(ns sparkling.api-test
  (:import [org.apache.spark.broadcast Broadcast])
  (:use clojure.test)
  (:require [sparkling.api :as f]
            [sparkling.conf :as conf]
            [sparkling.broadcast :as fb]
            ))




(deftest broadcast
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (f/with-context
      c conf
      (testing
        "gives us a Broadcast Var"
        (is (= (class Broadcast) (fb/broadcast c 'anything))))

      (testing
        "creates a JavaRDD"
        (is (= (fb/value (fb/broadcast c 'anything)) 'anything))))))
