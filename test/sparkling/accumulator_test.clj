(ns sparkling.accumulator-test
  (:import [org.apache.spark.util AccumulatorV2])
  (:require [clojure.test :refer :all]
            [sparkling.api :as s]
            [sparkling.conf :as conf]
            [sparkling.accumulator :as ac]))




(deftest accumulator
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "ac-test"))]
    (s/with-context
      c conf
      (testing
        "gives us an Accumulator Var"
        (is (instance? org.apache.spark.util.AccumulatorV2 (ac/accumulator c 0))))

      (testing
        "returns a value"
        (is (= (ac/value (ac/accumulator c 0.0)) 0.0)))

      (testing
        "returns a name"
        (is (= (ac/name (ac/accumulator c 0 "n")) "n")))

      (testing
        "foreach accumulates values"
        (let [a (ac/accumulator c 0.0)]
          (do
            (-> (s/parallelize c [1. 2. 3. 4. 5.5])
                (s/foreach (fn [x] (.add a x))))
            (is (= (ac/value a) 15.5))))))))

(deftest long-accumulator
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "ac-test"))]
    (s/with-context
      c conf
      (testing
          "gives us an Accumulator Var"
        (is (instance? org.apache.spark.util.AccumulatorV2 (ac/long-accumulator c 0))))

      (testing
          "returns a value"
        (is (= (ac/value (ac/long-accumulator c 0)) 0)))

      (testing
          "returns a name"
        (is (= (ac/name (ac/long-accumulator c 0 "n")) "n")))

      (testing
          "foreach accumulates values"
        (let [a (ac/long-accumulator c 0)]
          (do
            (-> (s/parallelize c [1 2 3 4 5])
                (s/foreach (fn [x] (.add a x))))
            (is (= (ac/value a) 15))))))))
