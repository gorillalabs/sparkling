(ns sparkling.core-test

  (:use clojure.test)
  (:require [clojure.set]
            [sparkling.core :as s]
            [sparkling.conf :as conf]
    ;; this is to have the reader macro sparkling/tuple defined
            [sparkling.destructuring :as sd]
            [sparkling.testutils :refer :all]
            ))




(deftest lookup
  (s/with-context
    c
    (-> (conf/spark-conf)
        (conf/set-sparkling-registrator)
        (conf/set "spark.kryo.registrationRequired" "true")
        (conf/master "local[*]")
        (conf/app-name "api-test"))
    (let [rdd (s/into-pair-rdd c [#sparkling/tuple[1 {:id 1 :a 1 :b 2}]
                                  #sparkling/tuple[2 {:id 2 :a 2 :b 2}]
                                  #sparkling/tuple[2 {:id 3 :a 2 :b 3}]
                                  #sparkling/tuple[3 {:id 4 :a 3 :b 3}]
                                  #sparkling/tuple[3 {:id 5 :a 3 :b 1}]])
          ]

      (testing
        "lookup existing single"
        (is (equals-ignore-order? (->>
                                    (s/lookup rdd 1)
                                    vec)
                                  [{:id 1 :a 1 :b 2}])))

      (testing
        "lookup existing multiple"
        (is (equals-ignore-order? (->>
                                    (s/lookup rdd 2)
                                    vec)
                                  [{:id 2 :a 2 :b 2}
                                   {:id 3 :a 2 :b 3}])))

      (testing
        "lookup non-existing"
        (is (equals-ignore-order? (->>
                                    (s/lookup rdd 4)
                                    vec)
                                  []))))))

(deftest transformations
  (s/with-context
    c
    (-> (conf/spark-conf)
        (conf/set-sparkling-registrator)
        (conf/set "spark.kryo.registrationRequired" "true")
        (conf/master "local[*]")
        (conf/app-name "api-test"))
    (let [rdd (s/into-pair-rdd c [#sparkling/tuple[1 {:id 1 :a 1 :b 2}]
                                  #sparkling/tuple[2 {:id 2 :a 2 :b 2}]
                                  #sparkling/tuple[2 {:id 3 :a 2 :b 3}]
                                  #sparkling/tuple[3 {:id 4 :a 3 :b 3}]
                                  #sparkling/tuple[3 {:id 5 :a 3 :b 1}]])
          empty-rdd (s/into-pair-rdd c [])
          other1 (s/into-pair-rdd c [#sparkling/tuple[1 nil]
                                     #sparkling/tuple[2 :dont-care]
                                     ])]

      (testing
        "intersect-by-key with empty rdd"
        (is (equals-ignore-order? (->>
                                    (s/intersect-by-key rdd identity identity empty-rdd)
                                       s/collect
                                       vec)
                                  [])))

      (testing
        "intersect-by-key with empty rdd"
        (is (equals-ignore-order? (->>
                                    (s/intersect-by-key rdd :b :a other1) ; keep items in rdd where :b-entries are keys in other-1
                                    s/collect
                                    vec)
                                  [#sparkling/tuple[1 {:id 1 :a 1 :b 2}]
                                   #sparkling/tuple[2 {:id 2 :a 2 :b 2}]
                                   #sparkling/tuple[3 {:id 5 :a 3 :b 1}]])))
      )))





















