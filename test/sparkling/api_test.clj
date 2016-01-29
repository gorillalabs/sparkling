(ns sparkling.api-test
  (:import [org.apache.spark HashPartitioner]
           [scala Some Tuple2]
           [org.apache.spark.api.java JavaSparkContext JavaRDD])
  (:use clojure.test)
  (:require [clojure.set]
            [sparkling.api :as s]
            [sparkling.conf :as conf]
            [sparkling.scalaInterop :as si]
    ;; this is to have the reader macro sparkling/tuple defined
            [sparkling.destructuring :as sd]
            [sparkling.serialization :as ser]
            [sparkling.kryoserializer :as ks]
            [sparkling.testutils.records.domain :as domain]
            [sparkling.testutils :refer :all]
            [sparkling.destructuring :as s-de]))



(deftest spark-context
  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "true")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (s/with-context c conf
                    (testing
                      "gives us a JavaSparkContext"
                      (is (= (class c) JavaSparkContext)))

                    (testing
                      "creates a JavaRDD"
                      (is (= (class (s/parallelize c [1 2 3 4 5])) JavaRDD)))

                    (testing
                      "round-trips a clojure vector"
                      (is (= (-> (s/parallelize c [1 2 3 4 5]) s/collect vec) [1 2 3 4 5]))))))

(deftest serializable-functions
  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))
        kryo (ks/kryo-serializer conf)                      ; ((ks/round-trip kryo (s/comp (partial * 2) inc)) 1)
        myfn (fn [x] (* 2 x))]
    #_(testing
      "inline op returns a serializable fn"
      (type myfn) => :serializable.fn/serializable-fn)

    (testing
      "we can serialize and deserialize it to a function"
      (is (fn? (ks/round-trip kryo myfn))))

    (testing (is (= 6
                    ((ks/round-trip kryo myfn) 3))))

    #_(testing
      "it round-trips back to a serializable fn"
      (type (-> myfn serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    #_(testing :comp
          "it round-trips back to a serializable fn (comp)"
          (type (-> (s/comp myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    #_(testing :comp
          "it round-trips back to a serializable fn (comp)"
          (type (-> (s/comp myfn myfn myfn myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    #_(testing :comp ;; this won't work due to a limitation in serializable-fn
          "it round-trips back to a serializable fn (comp)"
          (type (-> (s/comp myfn myfn myfn myfn myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    ))

(deftest transformations

  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "true")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (s/with-context c conf
                    (testing
                      "map returns an RDD formed by passing each element of the source RDD through a function"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 3 4 5])
                                                    (s/map (fn [x] (* 2 x)))
                                                    s/collect
                                                    vec) [2 4 6 8 10])))

                    (testing
                      "map-to-pair returns an RDD of (K, V) pairs formed by passing each element of the source
                      RDD through a pair function"
                      (is (equals-ignore-order? (-> (s/parallelize c ["a" "b" "c" "d"])
                                                    (s/map-to-pair (fn [x] (s/tuple x 1)))
                                                    (s/map (sd/key-value-fn identity-vec))
                                                    s/collect
                                                    vec) [["a" 1] ["b" 1] ["c" 1] ["d" 1]])))


                    (testing
                      "key-by returns an RDD of (K,V) pairs from an RDD of V elements formed by passing each V through a function to get to K."
                      (is (equals-ignore-order? (-> (s/parallelize c [0 1 2 3 4])
                                                    (s/key-by even?)
                                                    (s/map untuple)
                                                    s/collect
                                                    vec)
                                                [[true 0] [false 1] [true 2] [false 3] [true 4]])))

                    (testing
                      "reduce-by-key returns an RDD of (K, V) when called on an RDD of (K, V) pairs"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple ["key1" 1]
                                                                            #sparkling/tuple ["key1" 2]
                                                                            #sparkling/tuple ["key2" 3]
                                                                            #sparkling/tuple ["key2" 4]
                                                                            #sparkling/tuple ["key3" 5]])
                                                    (s/reduce-by-key +)
                                                    s/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 3]
                                                 ["key2" 7]
                                                 ["key3" 5]])))

                    (testing
                      "similar to map, but each input item can be mapped to 0 or more output items;
                      mapping function must therefore return a sequence rather than a single item"
                      (is (equals-ignore-order? (-> (s/parallelize c [["Four score and seven years ago our fathers"]
                                                                      ["brought forth on this continent a new nation"]])
                                                    (s/flat-map (fn [x] (clojure.string/split (first x) #" ")))
                                                    s/collect
                                                    vec)
                                                ["Four" "score" "and" "seven" "years" "ago" "our" "fathers" "brought" "forth" "on" "this" "continent" "a" "new" "nation"])))

                    (testing
                      "filter returns an RDD formed by selecting those elements of the source on which func returns true"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 3 4 5 6])
                                                    (s/filter even?)
                                                    s/collect
                                                    vec)
                                                [2 4 6])))


                    (testing
                      "cogroup returns an RDD of (K, (V, W)) pairs with all pairs of elements of each key when called on RDDs of type (K, V) and (K, W)"
                      (let [rdd (s/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                        #sparkling/tuple["key2" 2]
                                                        #sparkling/tuple["key3" 3]
                                                        #sparkling/tuple["key4" 4]
                                                        #sparkling/tuple["key5" 5]])
                            other1 (s/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                           #sparkling/tuple["key3" 33]
                                                           #sparkling/tuple["key4" 44]
                                                           #sparkling/tuple["key6" 66]
                                                           #sparkling/tuple["key6" 666]])
                            ]
                        (is (equals-ignore-order? (-> (s/cogroup rdd other1)
                                                      (s/map (sd/key-seq-seq-fn (fn [k v1 v2] [k [(vec$ v1) (vec$ v2)]])))
                                                      s/collect
                                                      vec)
                                                  [["key1" [[1] [11]]]
                                                   ["key2" [[2] nil]]
                                                   ["key3" [[3] [33]]]
                                                   ["key4" [[4] [44]]]
                                                   ["key5" [[5] nil]]
                                                   ["key6" [nil [66 666]]]
                                                   ]))))



                    (testing
                      "cogroup returns an RDD of (K, (V, W, X)) pairs with all pairs of elements of each key when called on RDDs of type (K, V), (K, W) and (K,X)"
                      (let [rdd (s/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                        #sparkling/tuple["key2" 2]
                                                        #sparkling/tuple["key3" 3]
                                                        #sparkling/tuple["key4" 4]
                                                        #sparkling/tuple["key5" 5]])
                            other1 (s/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                           #sparkling/tuple["key3" 33]
                                                           #sparkling/tuple["key4" 44]
                                                           #sparkling/tuple["key6" 66]
                                                           #sparkling/tuple["key6" 666]])
                            other2 (s/parallelize-pairs c [#sparkling/tuple["key1" 111]
                                                           #sparkling/tuple["key3" 333]
                                                           #sparkling/tuple["key5" 555]
                                                           ])
                            ]
                        (is (equals-ignore-order?
                              (-> (s/cogroup rdd other1 other2)
                                  (s/map (sd/key-seq-seq-seq-fn (fn [k v1 v2 v3] [k [(vec$ v1) (vec$ v2) (vec$ v3)]])))
                                  s/collect
                                  vec)
                              [["key1" [[1] [11] [111]]]
                               ["key2" [[2] nil nil]]
                               ["key3" [[3] [33] [333]]]
                               ["key4" [[4] [44] nil]]
                               ["key5" [[5] nil [555]]]
                               ["key6" [nil [66 666] nil]]
                               ]))))


                    (testing
                      "join returns an RDD of (K, (V, W)) pairs with all pairs of elements of each key when called on RDDs of type (K, V) and (K, W)"
                      (let [LDATA (s/parallelize-pairs c [#sparkling/tuple["key1" [2]]
                                                          #sparkling/tuple["key2" [3]]
                                                          #sparkling/tuple["key3" [5]]
                                                          #sparkling/tuple["key4" [1]]
                                                          #sparkling/tuple["key5" [2]]])
                            RDATA (s/parallelize-pairs c [#sparkling/tuple["key1" [22]]
                                                          #sparkling/tuple["key3" [33]]
                                                          #sparkling/tuple["key4" [44]]])
                            ]
                        (is (equals-ignore-order? (-> (s/join LDATA RDATA)
                                                      (s/map (sd/key-val-val-fn identity-vec))
                                                      s/collect
                                                      vec)
                                                  [["key3" [5] [33]]
                                                   ["key4" [1] [44]]
                                                   ["key1" [2] [22]]]))))

                    (testing
                      "left-outer-join returns an RDD of (K, (V, W)) when called on RDDs of type (K, V) and (K, W)"
                      (let [LDATA (s/parallelize-pairs c [#sparkling/tuple["key1" [2]]
                                                          #sparkling/tuple["key2" [3]]
                                                          #sparkling/tuple["key3" [5]]
                                                          #sparkling/tuple["key4" [1]]
                                                          #sparkling/tuple["key5" [2]]])
                            RDATA (s/parallelize-pairs c [#sparkling/tuple["key1" [22]]
                                                          #sparkling/tuple["key3" [33]]
                                                          #sparkling/tuple["key4" [44]]])]
                        (is (equals-ignore-order? (-> (s/left-outer-join LDATA RDATA)
                                                      (s/map (sd/key-val-val-fn identity-vec :optional-second-value? true))
                                                      s/collect
                                                      vec)
                                                  [["key3" [5] [33]]
                                                   ["key4" [1] [44]]
                                                   ["key5" [2] nil]
                                                   ["key1" [2] [22]]
                                                   ["key2" [3] nil]]))))


                    (testing
                      "union concats two RDDs"
                      (let [rdd1 (s/parallelize c [1 2 3 4])
                            rdd2 (s/parallelize c [11 12 13])]
                        (is (equals-ignore-order? (-> (s/union rdd1 rdd2)
                                                      s/collect
                                                      vec)
                                                  [1 2 3 4 11 12 13]))))

                    (testing
                      "union concats more than two RDDs"
                      (let [rdd1 (s/parallelize c [1 2 3 4])
                            rdd2 (s/parallelize c [11 12 13])
                            rdd3 (s/parallelize c [21 22 23])]
                        (is (equals-ignore-order?
                              (-> (s/union rdd1 rdd2 rdd3)
                                  s/collect
                                  vec)
                              [1 2 3 4 11 12 13 21 22 23]))))

                    (testing
                      "sample returns a fraction of the RDD, with/without replacement,
                      using a given random number generator seed"
                      (is (clojure.set/subset?
                            (apply hash-set
                                   (-> (s/parallelize c [0 1 2 3 4 5 6 7 8 9 10 11])
                                       (s/sample false 0.5 2)
                                       s/collect
                                       ))
                            #{0 1 2 3 4 5 6 7 8 9 10 11}
                            )))

                    (testing
                      "combine-by-key returns an RDD by combining the elements for each key using a custom
                      set of aggregation functions"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                                            #sparkling/tuple["key2" 1]
                                                                            #sparkling/tuple["key1" 1]])
                                                    (s/combine-by-key identity + +)
                                                    s/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 2] ["key2" 1]])))

                    (testing
                      "sort-by-key returns an RDD of (K, V) pairs sorted by keys in asc or desc order"
                      (is (= (-> (s/parallelize-pairs c [#sparkling/tuple[2 "aa"]
                                                         #sparkling/tuple[5 "bb"]
                                                         #sparkling/tuple[3 "cc"]
                                                         #sparkling/tuple[1 "dd"]])
                                 (s/sort-by-key compare false)
                                 s/collect
                                 untuple-all
                                 vec)
                             [[5 "bb"] [3 "cc"] [2 "aa"] [1 "dd"]])))

                    (testing
                      "coalesce"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 3 4 5])
                                                    (s/coalesce 1)
                                                    s/collect
                                                    vec) [1 2 3 4 5])))

                    (testing
                      "group-by returns an RDD of items grouped by the grouping function"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 1 2 3 5 8])
                                                    (s/group-by (fn [x] (mod x 2)))
                                                    s/collect
                                                    untuple-all
                                                    seq-values
                                                    vec
                                                    )
                                                [[0 [2 8]] [1 [1 1 3 5]]])))

                    (testing
                      "group-by-key"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                                            #sparkling/tuple["key1" 2]
                                                                            #sparkling/tuple["key2" 3]
                                                                            #sparkling/tuple["key2" 4]
                                                                            #sparkling/tuple["key3" 5]])
                                                    s/group-by-key
                                                    s/collect
                                                    untuple-all
                                                    seq-values
                                                    vec)
                                                [["key3" [5]] ["key1" [1 2]] ["key2" [3 4]]])))

                    (testing
                      "flat-map-to-pair"
                      (is (equals-ignore-order? (-> (s/parallelize c [["Four score and seven"]
                                                                      ["years ago"]])
                                                    (s/flat-map-to-pair (fn [x] (map (fn [y] (s/tuple y 1))
                                                                                     (clojure.string/split (first x) #" "))))
                                                    (s/map untuple)
                                                    s/collect
                                                    vec)
                                                [["Four" 1] ["score" 1] ["and" 1] ["seven" 1] ["years" 1] ["ago" 1]])))

                    (testing
                      "flat-map-values"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" [1 2]]
                                                                            #sparkling/tuple["key2" [3 4]]
                                                                            #sparkling/tuple["key3" [5]]])
                                                    (s/flat-map-values (fn [x] x))
                                                    s/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 1] ["key1" 2] ["key2" 3] ["key2" 4] ["key3" 5]])))


                    (testing
                      "map-partitions-to-pair"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" [1 2]]
                                                                            #sparkling/tuple["key2" [3 4]]
                                                                            #sparkling/tuple["key3" [5]]])
                                                    (s/map-partitions-to-pair
                                                      (fn [it]
                                                        (map
                                                          #(s/tuple (first (s-de/value %1)) (rest (s-de/value %1)))
                                                          (iterator-seq it)))
                                                      :preserves-partitioning false)
                                                    s/collect
                                                    untuple-all
                                                    vec
                                                    )
                                                [[1 [2]] [3 [4]] [5 []]])))


                    (testing
                      "map-partition"
                      (is (equals-ignore-order? (-> (s/parallelize c [0 1 2 3 4])
                                                    (s/map-partition (fn [it] (map identity (iterator-seq it))))
                                                    s/collect)
                                                [0 1 2 3 4])))

                    (testing
                      "map-partition-with-index"
                      (let [ret (-> (s/parallelize c [0 1 2 3 4])
                                    (s/repartition 4)
                                    (s/map-partition-with-index (fn [i it] (.iterator (map identity [i (iterator-seq it)]))))
                                    (s/collect)
                                    ((fn [key-value-list]
                                       (let [key-value-map (apply array-map key-value-list)]
                                         [(keys key-value-map)
                                          (apply concat (vals key-value-map))
                                          ]
                                         )))
                                    )
                            partitions (first ret)
                            values (second ret)]

                        (is (equals-ignore-order? partitions [0 1 2 3]))
                        (is (equals-ignore-order? values [0 1 2 3 4]))))

                    (testing
                      "cartesian creates cartesian product of two RDDS"
                      (let [rdd1 (s/parallelize c [1 2])
                            rdd2 (s/parallelize c [5 6 7])]
                        (is (equals-ignore-order? (-> (s/cartesian rdd1 rdd2)
                                                      s/collect
                                                      vec)
                                                  [#sparkling/tuple[1 5] #sparkling/tuple[1 6] #sparkling/tuple[1 7] #sparkling/tuple[2 5] #sparkling/tuple[2 6] #sparkling/tuple[2 7]]
                                                  ))))

                    (testing
                        "intersection returns the common elements of two vectors"
                      (let [rdd1 (s/parallelize c [1 2 3 4 5])
                            rdd2 (s/parallelize c [1 3 5 7])]
                        (is (equals-ignore-order? (-> (s/intersection rdd1 rdd2)
                                                      s/collect
                                                      vec)
                                                  [1 3 5]))))
                    (testing
                        "subtract returns elements in the first RDD that not present in the second"
                      (let [rdd1 (s/parallelize c [1 2 3 4 5])
                            rdd2 (s/parallelize c [1 3 5])]
                        (is (equals-ignore-order? (-> (s/subtract rdd1 rdd2)
                                                      s/collect
                                                      vec)
                                                  [2 4]))))

                    (testing
                        "subtract-by-key returns elements by key in the first RDD that not present in the second"
                      (let [rdd1 (s/parallelize-pairs c [#sparkling/tuple[1 4]
                                                         #sparkling/tuple[2 5]
                                                         #sparkling/tuple[3 6]])
                            rdd2 (s/parallelize-pairs c [#sparkling/tuple[1 1]
                                                         #sparkling/tuple[3 6]])]
                        (is (equals-ignore-order? (-> (s/subtract-by-key rdd1 rdd2)
                                                      s/collect
                                                      vec)
                                                  [#sparkling/tuple[2 5]]))))


                    ;TODO:                      (future-fact "repartition returns a new RDD with exactly n partitions")

                    )))

(deftest actions

  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "true")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (s/with-context c conf
                    (testing
                      "aggregates elements of RDD using a function that takes two arguments and returns one,
                      return type is a value"
                      (is (= (-> (s/parallelize c [1 2 3 4 5])
                                 (s/reduce +)) 15)))

                    (testing
                      "count-by-key returns a hashmap of (K, int) pairs with the count of each key; only available on RDDs of type (K, V)"
                      (is (= (-> (s/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                         #sparkling/tuple["key1" 2]
                                                         #sparkling/tuple["key2" 3]
                                                         #sparkling/tuple["key2" 4]
                                                         #sparkling/tuple["key3" 5]])
                                 (s/count-by-key))
                             {"key1" 2 "key2" 2 "key3" 1})))

                    (testing
                      "count-by-value returns a hashmap of (V, int) pairs with the count of each value"
                      (is (= (-> (s/parallelize c [["key1" 11]
                                                   ["key1" 11]
                                                   ["key2" 12]
                                                   ["key2" 12]
                                                   ["key3" 13]])
                                 (s/count-by-value)) {["key1" 11] 2, ["key2" 12] 2, ["key3" 13] 1})))

                    (testing
                      "values returns the values (V) of a hashmap of (K, V) pairs"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key3" 13]])
                                                    (s/values)
                                                    (s/collect)
                                                    vec)
                                                [11, 11, 12, 12, 13])))

                    (testing
                      "keys returns the keys (K) of a hashmap of (K, V) pairs"
                      (is (equals-ignore-order? (-> (s/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key3" 13]])
                                                    (s/keys)
                                                    (s/collect)
                                                    vec)
                                                ["key1" "key1" "key2" "key2" "key3"])))


                    (testing
                      "foreach runs a function on each element of the RDD, returns nil; this is usually done for side effcts"
                      (is (nil? (-> (s/parallelize c [1 2 3 4 5])
                                    (s/foreach (fn [x] x))))))

                    (testing
                      "foreach-partition runs a function on each partition iterator of RDD; basically for side effects like foreach"
                      (is (nil? (-> (s/parallelize c [1 2 3 4 5])
                                    (s/foreach-partition identity)))))

                    (testing
                      "fold returns aggregate each partition, and then the results for all the partitions, using a given associative function and a neutral 'zero value'"
                      (is (= (-> (s/parallelize c [1 2 3 4 5])
                                 (s/fold 0 +)) 15)))

                    (testing
                      "first returns the first element of an RDD"
                      (is (= (-> (s/parallelize c [1 2 3 4 5])
                                 s/first) 1)))

                    (testing
                      "count return the number of elements in an RDD"
                      (is (= (-> (s/parallelize c [["a" 1] ["b" 2] ["c" 3] ["d" 4] ["e" 5]])
                                 s/count) 5)))

                    (testing
                      "max returns the maximum element in an RDD in the ordering defined by the comparator"
                      (is (= 7 (->> (s/parallelize c [1 2 3 4 5 6 7 8 9])
                                    (s/max #(compare (mod %1 8) (mod %2 8)))))))

                    (testing
                      "min returns the minimum element in an RDD in the ordering defined by the comparator"
                      (is (= 8 (->> (s/parallelize c [1 2 3 4 5 6 7 8 9])
                                    (s/min #(compare (mod %1 8) (mod %2 8)))))))

                    (testing
                      "collect returns all elements of the RDD as an array at the driver program"
                      (is (equals-ignore-order? (-> (s/parallelize c [[1] [2] [3] [4] [5]])
                                                    s/collect
                                                    vec)
                                                [[1] [2] [3] [4] [5]])))

                    (testing
                      "distinct returns distinct elements of an RDD"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 1 3 4 5 4])
                                                    s/distinct
                                                    s/collect
                                                    vec)
                                                [1 2 3 4 5])))

                    (testing
                      "distinct returns distinct elements of an RDD with the given number of partitions"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 1 3 4 5 4])
                                                    (s/distinct 2)
                                                    s/collect
                                                    vec)
                                                [1 2 3 4 5])))

                    (testing
                      "take returns an array with the first n elements of an RDD"
                      (is (= (-> (s/parallelize c [1 2 3 4 5])
                                 (s/take 3))
                             [1 2 3])))

                    (testing
                      "glom returns an RDD created by coalescing all elements within each partition into a list"
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 3 4 5 6 7 8 9 10] 2)
                                                    s/glom
                                                    s/collect
                                                    vec)
                                                [[1 2 3 4 5] [6 7 8 9 10]]
                                                )))

                    (testing
                      "cache persists this RDD with a default storage level (MEMORY_ONLY)"
                      (let [cache (-> (s/parallelize c [1 2 3 4 5])
                                      (s/cache))]
                        (is (= (-> cache
                                   s/collect) [1 2 3 4 5]))))

                    (testing
                        "uncache releases this RDD from storage"
                      (let [rdd (-> (s/parallelize c [1 2 3 4 5])
                                    (s/cache))]
                        (s/collect rdd)
                        (s/uncache true rdd)
                        (is (= (:none s/STORAGE-LEVELS)
                               (.getStorageLevel rdd)))))

                    (testing
                      "histogram uses bucketCount number of evenly-spaced buckets"
                      (is (= (-> (s/parallelize c [1.0 2.2 2.6 3.3 3.5 3.7 4.4 4.8 5.5 6.0])
                                 (s/histogram 5))
                             [[1.0 2.0 3.0 4.0 5.0 6.0] [1 2 3 2 2]])))

                    (testing
                      "histogram uses the provided buckets"
                      (is (= (-> (s/parallelize c [1.0 2.2 2.6 3.3 3.5 3.7 4.4 4.8 5.5 6.0])
                                 (s/histogram [1.0 4.0 6.0]))
                             [6 4])))
                    )))



(deftest
  partitioning

  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "true")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (s/with-context c conf

                    (testing
                      "partitions returns a vec of partitions for a given RDD"
                      (is (= (-> (s/parallelize c [1 2 3 4 5 6 7 8 9 10] 2)
                                 s/partitions
                                 count) 2)))

                    (testing
                      "partition-by partitions a given RDD according to the partitioning-fn using a hash partitioner."
                      (is (equals-ignore-order? (-> (s/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                                                    (s/map-to-pair (fn [x] (s/tuple x x)))
                                                    (s/partition-by (s/hash-partitioner 2))
                                                    s/glom
                                                    s/collect
                                                    vec)
                                                [[#sparkling/tuple[2 2] #sparkling/tuple[4 4] #sparkling/tuple[6 6] #sparkling/tuple[8 8] #sparkling/tuple[10 10]]
                                                 [#sparkling/tuple[1 1] #sparkling/tuple[3 3] #sparkling/tuple[5 5] #sparkling/tuple[7 7] #sparkling/tuple[9 9]]])))




                    (testing
                      "partition-by returns an RDD with a hash partitioner."
                      (is #(some-instance? HashPartitioner %1)
                          (-> (s/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                              (s/map-to-pair (fn [x] (s/tuple x x)))
                              (s/partition-by (s/hash-partitioner 2))
                              (s/partitioner)
                              )))


                    (testing
                      "map-values keeps the hash partitioner."
                      (is #(some-instance? HashPartitioner %1)
                          (-> (s/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                              (s/map-to-pair (fn [x] (s/tuple x x)))
                              (s/partition-by (s/hash-partitioner 2))
                              (s/map-values #(* %1 2))
                              (s/partitioner)
                              )))


                    (testing
                      "partition-by partitions a given RDD according to the partitioning-fn using a hash partitioner."
                      (is (equals-ignore-order?
                            (-> (s/parallelize-pairs c
                                                     [#sparkling/tuple[{:a 1 :b 1} 11] ;; for me, (mod (hash 1) 2) and (mod (hash 3) 2) return 0 and 1 respectively, resulting in splitting the rdd in two partitions based upon :b
                                                      #sparkling/tuple[{:a 2 :b 1} 11]
                                                      #sparkling/tuple[{:a 3 :b 1} 12]
                                                      #sparkling/tuple[{:a 4 :b 3} 12]
                                                      #sparkling/tuple[{:a 5 :b 3} 13]] 1)
                                (s/partition-by (s/hash-partitioner
                                                  :b
                                                  2))
                                s/glom
                                s/collect
                                vec)
                            [; this is partition 1:
                             [#sparkling/tuple[{:a 1 :b 1} 11]
                              #sparkling/tuple[{:a 2 :b 1} 11]
                              #sparkling/tuple[{:a 3 :b 1} 12]]
                             ; and this is partition 2:
                             [#sparkling/tuple[{:a 4 :b 3} 12]
                              #sparkling/tuple[{:a 5 :b 3} 13]]]))

                      )


                    (let [b-partitioner (s/hash-partitioner
                                          :b
                                          2)
                          rdd (s/parallelize-pairs c
                                                   [#sparkling/tuple[{:a 1 :b 1} {:a 1 :b 1 :c 1}]
                                                    #sparkling/tuple[{:a 2 :b 1} {:a 2 :b 1 :c 2}]
                                                    #sparkling/tuple[{:a 3 :b 1} {:a 3 :b 1 :c 3}]
                                                    #sparkling/tuple[{:a 4 :b 3} {:a 4 :b 3 :c 4}]
                                                    #sparkling/tuple[{:a 5 :b 3} {:a 5 :b 3 :c 5}]] 1)
                          re-partitioned-rdd (->
                                               rdd
                                               (s/partition-by b-partitioner)
                                               (s/rekey-preserving-partitioning-without-check
                                                 (sd/key-value-fn
                                                   (fn [_ value] (s/tuple (select-keys value [:b :c]) value)))))]
                      (testing
                        "rekey keeps the hash partitioner if told to"
                        (is (= b-partitioner (s/partitioner re-partitioned-rdd))))

                      (testing
                        "rekey does keep all elements"
                        (is (= (s/collect re-partitioned-rdd)

                               [#sparkling/tuple[{:c 1 :b 1} {:a 1 :b 1 :c 1}]
                                #sparkling/tuple[{:c 2 :b 1} {:a 2 :b 1 :c 2}]
                                #sparkling/tuple[{:c 3 :b 1} {:a 3 :b 1 :c 3}]
                                #sparkling/tuple[{:c 4 :b 3} {:a 4 :b 3 :c 4}]
                                #sparkling/tuple[{:c 5 :b 3} {:a 5 :b 3 :c 5}]]
                               )))



                      (testing
                        "union concats two identical RDDs "
                        (is (equals-ignore-order? (-> (s/partitioner-aware-union re-partitioned-rdd re-partitioned-rdd)
                                                      s/collect
                                                      vec)
                                                  [#sparkling/tuple[{:c 1 :b 1} {:a 1 :b 1 :c 1}]
                                                   #sparkling/tuple[{:c 2 :b 1} {:a 2 :b 1 :c 2}]
                                                   #sparkling/tuple[{:c 3 :b 1} {:a 3 :b 1 :c 3}]
                                                   #sparkling/tuple[{:c 4 :b 3} {:a 4 :b 3 :c 4}]
                                                   #sparkling/tuple[{:c 5 :b 3} {:a 5 :b 3 :c 5}]
                                                   #sparkling/tuple[{:c 1 :b 1} {:a 1 :b 1 :c 1}]
                                                   #sparkling/tuple[{:c 2 :b 1} {:a 2 :b 1 :c 2}]
                                                   #sparkling/tuple[{:c 3 :b 1} {:a 3 :b 1 :c 3}]
                                                   #sparkling/tuple[{:c 4 :b 3} {:a 4 :b 3 :c 4}]
                                                   #sparkling/tuple[{:c 5 :b 3} {:a 5 :b 3 :c 5}]
                                                   ])))


                      (testing
                        "union keeps the hash partitioner if told to"
                        (is (= b-partitioner (s/partitioner (s/partitioner-aware-union re-partitioned-rdd re-partitioned-rdd)))))





                      #_(testing
                        "union keeps the hash partitioner if told to"
                        (is (= b-partitioner (s/partitioner
                                               (org.apache.spark.api.java.JavaPairRDD/fromRDD
                                               (PartitionerAwareUnionRDD.
                                                              (.sc c)
                                                              (JavaConversions/asScalaBuffer [(.rdd re-partitioned-rdd) (.rdd re-partitioned-rdd)])
                                                              (.apply ClassTag$/MODULE$ java.lang.Object)
                                                              )
                                               (.apply ClassTag$/MODULE$ java.lang.Object)
                                               (.apply ClassTag$/MODULE$ java.lang.Object)
                                               )))))



                      #_(testing
                        "union concats more than two RDDs"
                        (let [rdd1 (s/parallelize c [1 2 3 4])
                              rdd2 (s/parallelize c [11 12 13])
                              rdd3 (s/parallelize c [21 22 23])]
                          (is (equals-ignore-order?
                                (-> (s/union rdd1 rdd2 rdd3)
                                    s/collect
                                    vec)
                                [1 2 3 4 11 12 13 21 22 23]))))

                      )

                    )))



(deftest record-partitioner

  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator" "sparkling.testutils.records.registrator.Registrator")
                 (conf/master "local[*]")
                 (conf/app-name "hadoop-record-test"))]
    (s/with-context c conf

                    (let [time-partitioner (s/hash-partitioner
                                             :timestamp
                                             2)
                          tweets [(domain/map->tweet {:username "miguno", :tweet "Rock: Nerf paper, scissors is fine.", :timestamp 1366150681})
                                  (domain/map->tweet {:username "BlizzardCS", :tweet "Works as intended.  Terran is IMBA.", :timestamp 1366154481})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "From the shadows I come!", :timestamp 1366154681})
                                  (domain/map->tweet {:username "VoidRay", :tweet "Prismatic core online!", :timestamp 1366160000})
                                  (domain/map->tweet {:username "VoidRay", :tweet "Fire at will, commander.", :timestamp 1366160010})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "I am the blade of Shakuras!", :timestamp 1366174681})
                                  (domain/map->tweet {:username "Immortal", :tweet "I return to serve!", :timestamp 1366175681})
                                  (domain/map->tweet {:username "Immortal", :tweet "En Taro Adun!", :timestamp 1366176283})
                                  (domain/map->tweet {:username "VoidRay", :tweet "There is no greater void than the one between your ears.", :timestamp 1366176300})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "I strike from the shadows!", :timestamp 1366184681})]
                          rdd (-> (s/parallelize c
                                                 tweets 1)
                                  (s/map-to-pair (fn [tweet] (s/tuple tweet tweet)))
                                  )

                          partitioned-rdd (->
                                            rdd
                                            (s/partition-by time-partitioner)
                                            )]
                      (testing
                        "rekey keeps the hash partitioner if told to"
                        (is (= time-partitioner (s/partitioner partitioned-rdd))))

                      (testing
                        "rekey does keep all elements"
                        (is (equals-ignore-order?
                              (s/collect (s/values partitioned-rdd))
                               tweets
                               )))))))



(defn test-fn [x] (domain/time x))

(deftest protocol-partitioner

  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator" "sparkling.testutils.records.registrator.Registrator")
                 (conf/master "local[*]")
                 (conf/app-name "hadoop-record-test"))]
    (s/with-context c conf

                    (let [time-partitioner (s/hash-partitioner
                                             test-fn
                                             2)
                          tweets [(domain/map->tweet {:username "miguno", :tweet "Rock: Nerf paper, scissors is fine.", :timestamp 1366150681})
                                  (domain/map->tweet {:username "BlizzardCS", :tweet "Works as intended.  Terran is IMBA.", :timestamp 1366154481})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "From the shadows I come!", :timestamp 1366154681})
                                  (domain/map->tweet {:username "VoidRay", :tweet "Prismatic core online!", :timestamp 1366160000})
                                  (domain/map->tweet {:username "VoidRay", :tweet "Fire at will, commander.", :timestamp 1366160010})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "I am the blade of Shakuras!", :timestamp 1366174681})
                                  (domain/map->tweet {:username "Immortal", :tweet "I return to serve!", :timestamp 1366175681})
                                  (domain/map->tweet {:username "Immortal", :tweet "En Taro Adun!", :timestamp 1366176283})
                                  (domain/map->tweet {:username "VoidRay", :tweet "There is no greater void than the one between your ears.", :timestamp 1366176300})
                                  (domain/map->tweet {:username "DarkTemplar", :tweet "I strike from the shadows!", :timestamp 1366184681})]
                          rdd (-> (s/parallelize c
                                                 tweets 1)
                                  (s/map-to-pair (fn [tweet] (s/tuple tweet tweet)))
                                  )

                          partitioned-rdd (->
                                            rdd
                                            (s/partition-by time-partitioner)
                                            )]
                      (testing
                        "rekey keeps the hash partitioner if told to"
                        (is (= time-partitioner (s/partitioner partitioned-rdd))))

                      (testing
                        "rekey does keep all elements"
                        (is (equals-ignore-order?
                              (s/collect (s/values partitioned-rdd))
                              tweets
                              )))))))


(deftest
  other-stuff

  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))
        kryo (ks/kryo-serializer conf)]
    (s/with-context c conf

                    (testing
                      "roundtrip composed fns"
                      (is (= ((ks/round-trip kryo (comp (partial * 2) inc)) 1) 4))
                      )
                    )))



