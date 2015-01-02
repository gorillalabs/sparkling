(ns sparkling.api-test
  (:import [org.apache.spark HashPartitioner]
           [scala None Some]
           [org.apache.spark.api.java JavaSparkContext JavaRDD]
           [org.apache.spark.rdd PartitionerAwareUnionRDD]
           [scala.collection JavaConversions]
           [scala.reflect ClassTag$])
  (:use clojure.test)
  (:require [sparkling.api :as f]
            [sparkling.conf :as conf]
            [sparkling.scalaInterop :as si]
            ;; this is to have the reader macro sparkling/tuple defined
            [sparkling.destructuring :as fd]
            [sparkling.kryoserializer :as ks]
            ))

(defn equals-ignore-order? [c1 c2]
  (= (frequencies c1) (frequencies c2)))


(defn untuple-all [coll]
  (map f/untuple coll))

(defn seq-values [coll]
  (map f/seq-value coll))

(defn untuple-values [coll]
  (map f/untuple-value coll))

(defn optional-second-values [coll]
  (map f/optional-second-value coll))

(defn some-instance? [cls option]
  (and (instance? Some option) (instance? cls (.get option))))


(defn identity-vec [& args]
              (vec args))

(defn vec$ [x]
  (when x (vec x)))

(deftest spark-context
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (f/with-context c conf
                    (testing
                      "gives us a JavaSparkContext"
                      (is (= (class c) JavaSparkContext)))

                    (testing
                      "creates a JavaRDD"
                      (is (= (class (f/parallelize c [1 2 3 4 5])) JavaRDD)))

                    (testing
                      "round-trips a clojure vector"
                      (is (= (-> (f/parallelize c [1 2 3 4 5]) f/collect vec) [1 2 3 4 5]))))))

(deftest serializable-functions

  (let [kryo (ks/kryo-serializer)                           ; ((ks/round-trip kryo (f/comp (partial * 2) inc)) 1)
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
          (type (-> (f/comp myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    #_(testing :comp
          "it round-trips back to a serializable fn (comp)"
          (type (-> (f/comp myfn myfn myfn myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    #_(testing :comp ;; this won't work due to a limitation in serializable-fn
          "it round-trips back to a serializable fn (comp)"
          (type (-> (f/comp myfn myfn myfn myfn myfn) serializable.fn/serialize serializable.fn/deserialize)) => :serializable.fn/serializable-fn)

    ))

(deftest untupling

  (testing
    "untuple returns a 2 vector"
    (let [tuple2 (scala.Tuple2. 1 "hi")]
      (is (= (f/untuple tuple2) [1 "hi"]))))

  (testing
    "double untuple returns a vector with a key and a 2 vector value"
    (let [double-tuple2 (scala.Tuple2. 1 (scala.Tuple2. 2 "hi"))]
      (is (= (f/double-untuple double-tuple2) [1 [2 "hi"]])))))

(deftest transformations

  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (f/with-context c conf
                    (testing
                      "map returns an RDD formed by passing each element of the source RDD through a function"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 3 4 5])
                                                    (f/map (fn [x] (* 2 x)))
                                                    f/collect
                                                    vec) [2 4 6 8 10])))

                    (testing
                      "map-to-pair returns an RDD of (K, V) pairs formed by passing each element of the source
                      RDD through a pair function"
                      (is (equals-ignore-order? (-> (f/parallelize c ["a" "b" "c" "d"])
                                                    (f/map-to-pair (fn [x] (f/tuple x 1)))
                                                    (f/map (fd/tuple-fn identity-vec))
                                                    f/collect
                                                    vec) [["a" 1] ["b" 1] ["c" 1] ["d" 1]])))


                    (testing
                      "key-by returns an RDD of (K,V) pairs from an RDD of V elements formed by passing each V through a function to get to K."
                      (is (equals-ignore-order? (-> (f/parallelize c [0 1 2 3 4])
                                                    (f/key-by even?)
                                                    (f/map f/untuple)
                                                    f/collect
                                                    vec)
                                                [[true 0] [false 1] [true 2] [false 3] [true 4]])))

                    (testing
                      "reduce-by-key returns an RDD of (K, V) when called on an RDD of (K, V) pairs"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple ["key1" 1]
                                                                            #sparkling/tuple ["key1" 2]
                                                                            #sparkling/tuple ["key2" 3]
                                                                            #sparkling/tuple ["key2" 4]
                                                                            #sparkling/tuple ["key3" 5]])
                                                    (f/reduce-by-key (fn [x y] (+ x y)))
                                                    f/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 3]
                                                 ["key2" 7]
                                                 ["key3" 5]])))

                    (testing
                      "similar to map, but each input item can be mapped to 0 or more output items;
                      mapping function must therefore return a sequence rather than a single item"
                      (is (equals-ignore-order? (-> (f/parallelize c [["Four score and seven years ago our fathers"]
                                                                      ["brought forth on this continent a new nation"]])
                                                    (f/flat-map (fn [x] (clojure.string/split (first x) #" ")))
                                                    f/collect
                                                    vec)
                                                ["Four" "score" "and" "seven" "years" "ago" "our" "fathers" "brought" "forth" "on" "this" "continent" "a" "new" "nation"])))

                    (testing
                      "filter returns an RDD formed by selecting those elements of the source on which func returns true"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 3 4 5 6])
                                                    (f/filter (fn [x] (even? x)))
                                                    f/collect
                                                    vec)
                                                [2 4 6])))


                    (testing
                      "cogroup returns an RDD of (K, (V, W)) pairs with all pairs of elements of each key when called on RDDs of type (K, V) and (K, W)"
                      (let [rdd (f/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                        #sparkling/tuple["key2" 2]
                                                        #sparkling/tuple["key3" 3]
                                                        #sparkling/tuple["key4" 4]
                                                        #sparkling/tuple["key5" 5]])
                            other1 (f/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                           #sparkling/tuple["key3" 33]
                                                           #sparkling/tuple["key4" 44]
                                                           #sparkling/tuple["key6" 66]
                                                           #sparkling/tuple["key6" 666]])
                            ]
                        (is (equals-ignore-order? (-> (f/cogroup rdd other1)
                                                      (f/map (fd/cogroup-2-fn (fn [k v1 v2] [k [(vec$ v1) (vec$ v2)]])))
                                                      f/collect
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
                      (let [rdd (f/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                        #sparkling/tuple["key2" 2]
                                                        #sparkling/tuple["key3" 3]
                                                        #sparkling/tuple["key4" 4]
                                                        #sparkling/tuple["key5" 5]])
                            other1 (f/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                           #sparkling/tuple["key3" 33]
                                                           #sparkling/tuple["key4" 44]
                                                           #sparkling/tuple["key6" 66]
                                                           #sparkling/tuple["key6" 666]])
                            other2 (f/parallelize-pairs c [#sparkling/tuple["key1" 111]
                                                           #sparkling/tuple["key3" 333]
                                                           #sparkling/tuple["key5" 555]
                                                           ])
                            ]
                        (is (equals-ignore-order?
                              (-> (f/cogroup rdd other1 other2)
                                  (f/map (fd/cogroup-3-fn (fn [k v1 v2 v3] [k [(vec$ v1) (vec$ v2) (vec$ v3)]])))
                                  f/collect
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
                      (let [LDATA (f/parallelize-pairs c [#sparkling/tuple["key1" [2]]
                                                          #sparkling/tuple["key2" [3]]
                                                          #sparkling/tuple["key3" [5]]
                                                          #sparkling/tuple["key4" [1]]
                                                          #sparkling/tuple["key5" [2]]])
                            RDATA (f/parallelize-pairs c [#sparkling/tuple["key1" [22]]
                                                          #sparkling/tuple["key3" [33]]
                                                          #sparkling/tuple["key4" [44]]])
                            ]
                        (is (equals-ignore-order? (-> (f/join LDATA RDATA)
                                                      (f/map (fd/tuple-value-fn identity-vec))
                                                      f/collect
                                                      vec)
                                                  [["key3" [5] [33]]
                                                   ["key4" [1] [44]]
                                                   ["key1" [2] [22]]]))))

                    (testing
                      "left-outer-join returns an RDD of (K, (V, W)) when called on RDDs of type (K, V) and (K, W)"
                      (let [LDATA (f/parallelize-pairs c [#sparkling/tuple["key1" [2]]
                                                          #sparkling/tuple["key2" [3]]
                                                          #sparkling/tuple["key3" [5]]
                                                          #sparkling/tuple["key4" [1]]
                                                          #sparkling/tuple["key5" [2]]])
                            RDATA (f/parallelize-pairs c [#sparkling/tuple["key1" [22]]
                                                          #sparkling/tuple["key3" [33]]
                                                          #sparkling/tuple["key4" [44]]])]
                        (is (equals-ignore-order? (-> (f/left-outer-join LDATA RDATA)
                                                      (f/map (fd/tuple-value-fn identity-vec :optional-second-value? true))
                                                      f/collect
                                                      vec)
                                                  [["key3" [5] [33]]
                                                   ["key4" [1] [44]]
                                                   ["key5" [2] nil]
                                                   ["key1" [2] [22]]
                                                   ["key2" [3] nil]]))))


                    (testing
                      "union concats two RDDs"
                      (let [rdd1 (f/parallelize c [1 2 3 4])
                            rdd2 (f/parallelize c [11 12 13])]
                        (is (equals-ignore-order? (-> (f/union rdd1 rdd2)
                                                      f/collect
                                                      vec)
                                                  [1 2 3 4 11 12 13]))))

                    (testing
                      "union concats more than two RDDs"
                      (let [rdd1 (f/parallelize c [1 2 3 4])
                            rdd2 (f/parallelize c [11 12 13])
                            rdd3 (f/parallelize c [21 22 23])]
                        (is (equals-ignore-order?
                              (-> (f/union rdd1 rdd2 rdd3)
                                  f/collect
                                  vec)
                              [1 2 3 4 11 12 13 21 22 23]))))

                    (testing
                      "sample returns a fraction of the RDD, with/without replacement,
                      using a given random number generator seed"
                      (is (#(<= 1 %1 2)
                            (-> (f/parallelize c [0 1 2 3 4 5 6 7 8 9])
                                (f/sample false 0.1 2)
                                f/collect
                                vec
                                count)
                            )))

                    (testing
                      "combine-by-key returns an RDD by combining the elements for each key using a custom
                      set of aggregation functions"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                                            #sparkling/tuple["key2" 1]
                                                                            #sparkling/tuple["key1" 1]])
                                                    (f/combine-by-key identity + +)
                                                    f/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 2] ["key2" 1]])))

                    (testing
                      "sort-by-key returns an RDD of (K, V) pairs sorted by keys in asc or desc order"
                      (is (= (-> (f/parallelize-pairs c [#sparkling/tuple[2 "aa"]
                                                         #sparkling/tuple[5 "bb"]
                                                         #sparkling/tuple[3 "cc"]
                                                         #sparkling/tuple[1 "dd"]])
                                 (f/sort-by-key compare false)
                                 f/collect
                                 untuple-all
                                 vec)
                             [[5 "bb"] [3 "cc"] [2 "aa"] [1 "dd"]])))

                    (testing
                      "coalesce"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 3 4 5])
                                                    (f/coalesce 1)
                                                    f/collect
                                                    vec) [1 2 3 4 5])))

                    (testing
                      "group-by returns an RDD of items grouped by the grouping function"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 1 2 3 5 8])
                                                    (f/group-by (fn [x] (mod x 2)))
                                                    f/collect
                                                    untuple-all
                                                    seq-values
                                                    vec
                                                    )
                                                [[0 [2 8]] [1 [1 1 3 5]]])))

                    (testing
                      "group-by-key"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                                            #sparkling/tuple["key1" 2]
                                                                            #sparkling/tuple["key2" 3]
                                                                            #sparkling/tuple["key2" 4]
                                                                            #sparkling/tuple["key3" 5]])
                                                    f/group-by-key
                                                    f/collect
                                                    untuple-all
                                                    seq-values
                                                    vec)
                                                [["key3" [5]] ["key1" [1 2]] ["key2" [3 4]]])))

                    (testing
                      "flat-map-to-pair"
                      (is (equals-ignore-order? (-> (f/parallelize c [["Four score and seven"]
                                                                      ["years ago"]])
                                                    (f/flat-map-to-pair (fn [x] (map (fn [y] (f/tuple y 1))
                                                                                       (clojure.string/split (first x) #" "))))
                                                    (f/map f/untuple)
                                                    f/collect
                                                    vec)
                                                [["Four" 1] ["score" 1] ["and" 1] ["seven" 1] ["years" 1] ["ago" 1]])))

                    (testing
                      "flat-map-values"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple["key1" [1 2]]
                                                                            #sparkling/tuple["key2" [3 4]]
                                                                            #sparkling/tuple["key3" [5]]])
                                                    (f/flat-map-values (fn [x] x))
                                                    f/collect
                                                    untuple-all
                                                    vec)
                                                [["key1" 1] ["key1" 2] ["key2" 3] ["key2" 4] ["key3" 5]])))

                    (testing
                      "map-partition"
                      (is (equals-ignore-order? (-> (f/parallelize c [0 1 2 3 4])
                                                    (f/map-partition (fn [it] (map identity (iterator-seq it))))
                                                    f/collect)
                                                [0 1 2 3 4])))

                    (testing
                      "map-partition-with-index"
                      (let [ret (-> (f/parallelize c [0 1 2 3 4])
                                    (f/repartition 4)
                                    (f/map-partition-with-index (fn [i it] (.iterator (map identity [i (iterator-seq it)]))))
                                    (f/collect)
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
                      (let [rdd1 (f/parallelize c [1 2])
                            rdd2 (f/parallelize c [5 6 7])]
                        (is (equals-ignore-order? (-> (f/cartesian rdd1 rdd2)
                                                      f/collect
                                                      vec)
                                                  [#sparkling/tuple[1 5] #sparkling/tuple[1 6] #sparkling/tuple[1 7] #sparkling/tuple[2 5] #sparkling/tuple[2 6] #sparkling/tuple[2 7]]
                                                  ))))


                    ;TODO:                      (future-fact "repartition returns a new RDD with exactly n partitions")

                    )))

(deftest actions

  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (f/with-context c conf
                    (testing
                      "aggregates elements of RDD using a function that takes two arguments and returns one,
                      return type is a value"
                      (is (= (-> (f/parallelize c [1 2 3 4 5])
                                 (f/reduce (fn [x y] (+ x y)))) 15)))

                    (testing
                      "count-by-key returns a hashmap of (K, int) pairs with the count of each key; only available on RDDs of type (K, V)"
                      (is (= (-> (f/parallelize-pairs c [#sparkling/tuple["key1" 1]
                                                         #sparkling/tuple["key1" 2]
                                                         #sparkling/tuple["key2" 3]
                                                         #sparkling/tuple["key2" 4]
                                                         #sparkling/tuple["key3" 5]])
                                 (f/count-by-key))
                             {"key1" 2 "key2" 2 "key3" 1})))

                    (testing
                      "count-by-value returns a hashmap of (V, int) pairs with the count of each value"
                      (is (= (-> (f/parallelize c [["key1" 11]
                                                   ["key1" 11]
                                                   ["key2" 12]
                                                   ["key2" 12]
                                                   ["key3" 13]])
                                 (f/count-by-value)) {["key1" 11] 2, ["key2" 12] 2, ["key3" 13] 1})))

                    (testing
                      "values returns the values (V) of a hashmap of (K, V) pairs"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key3" 13]])
                                                    (f/values)
                                                    (f/collect)
                                                    vec)
                                                [11, 11, 12, 12, 13])))

                    (testing
                      "keys returns the keys (K) of a hashmap of (K, V) pairs"
                      (is (equals-ignore-order? (-> (f/parallelize-pairs c [#sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key1" 11]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key2" 12]
                                                                            #sparkling/tuple["key3" 13]])
                                                    (f/keys)
                                                    (f/collect)
                                                    vec)
                                                ["key1" "key1" "key2" "key2" "key3"])))


                    (testing
                      "foreach runs a function on each element of the RDD, returns nil; this is usually done for side effcts"
                      (is (nil? (-> (f/parallelize c [1 2 3 4 5])
                                    (f/foreach (fn [x] x))))))

                    (testing
                      "foreach-partition runs a function on each partition iterator of RDD; basically for side effects like foreach"
                      (is (nil? (-> (f/parallelize c [1 2 3 4 5])
                                    (f/foreach-partition identity)))))

                    (testing
                      "fold returns aggregate each partition, and then the results for all the partitions, using a given associative function and a neutral 'zero value'"
                      (is (= (-> (f/parallelize c [1 2 3 4 5])
                                 (f/fold 0 (fn [x y] (+ x y)))) 15)))

                    (testing
                      "first returns the first element of an RDD"
                      (is (= (-> (f/parallelize c [1 2 3 4 5])
                                 f/first) 1)))

                    (testing
                      "count return the number of elements in an RDD"
                      (is (= (-> (f/parallelize c [["a" 1] ["b" 2] ["c" 3] ["d" 4] ["e" 5]])
                                 f/count) 5)))

                    (testing
                      "collect returns all elements of the RDD as an array at the driver program"
                      (is (equals-ignore-order? (-> (f/parallelize c [[1] [2] [3] [4] [5]])
                                                    f/collect
                                                    vec)
                                                [[1] [2] [3] [4] [5]])))

                    (testing
                      "distinct returns distinct elements of an RDD"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 1 3 4 5 4])
                                                    f/distinct
                                                    f/collect
                                                    vec)
                                                [1 2 3 4 5])))

                    (testing
                      "distinct returns distinct elements of an RDD with the given number of partitions"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 1 3 4 5 4])
                                                    (f/distinct 2)
                                                    f/collect
                                                    vec)
                                                [1 2 3 4 5])))

                    (testing
                      "take returns an array with the first n elements of an RDD"
                      (is (= (-> (f/parallelize c [1 2 3 4 5])
                                 (f/take 3))
                             [1 2 3])))

                    (testing
                      "glom returns an RDD created by coalescing all elements within each partition into a list"
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 3 4 5 6 7 8 9 10] 2)
                                                    f/glom
                                                    f/collect
                                                    vec)
                                                [[1 2 3 4 5] [6 7 8 9 10]]
                                                )))

                    (testing
                      "cache persists this RDD with a default storage level (MEMORY_ONLY)"
                      (let [cache (-> (f/parallelize c [1 2 3 4 5])
                                      (f/cache))]
                        (is (= (-> cache
                                   f/collect) [1 2 3 4 5]))))

                    (testing
                      "histogram uses bucketCount number of evenly-spaced buckets"
                      (is (= (-> (f/parallelize c [1.0 2.2 2.6 3.3 3.5 3.7 4.4 4.8 5.5 6.0])
                                 (f/histogram 5))
                             [[1.0 2.0 3.0 4.0 5.0 6.0] [1 2 3 2 2]])))

                    (testing
                      "histogram uses the provided buckets"
                      (is (= (-> (f/parallelize c [1.0 2.2 2.6 3.3 3.5 3.7 4.4 4.8 5.5 6.0])
                                 (f/histogram [1.0 4.0 6.0]))
                             [6 4])))
                    )))



(deftest
  partitioning

  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))]
    (f/with-context c conf

                    (testing
                      "partitions returns a vec of partitions for a given RDD"
                      (is (= (-> (f/parallelize c [1 2 3 4 5 6 7 8 9 10] 2)
                                 f/partitions
                                 count) 2)))

                    (testing
                      "partition-by partitions a given RDD according to the partitioning-fn using a hash partitioner."
                      (is (equals-ignore-order? (-> (f/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                                                    (f/map-to-pair (fn [x] (f/tuple x x)))
                                                    (f/partition-by (f/hash-partitioner 2))
                                                    f/glom
                                                    f/collect
                                                    vec)
                                                [[#sparkling/tuple[2 2] #sparkling/tuple[4 4] #sparkling/tuple[6 6] #sparkling/tuple[8 8] #sparkling/tuple[10 10]]
                                                 [#sparkling/tuple[1 1] #sparkling/tuple[3 3] #sparkling/tuple[5 5] #sparkling/tuple[7 7] #sparkling/tuple[9 9]]])))




                    (testing
                      "partition-by returns an RDD with a hash partitioner."
                      (is #(some-instance? HashPartitioner %1)
                          (-> (f/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                              (f/map-to-pair (fn [x] (f/tuple x x)))
                              (f/partition-by (f/hash-partitioner 2))
                              (f/partitioner)
                              )))


                    (testing
                      "map-values keeps the hash partitioner."
                      (is #(some-instance? HashPartitioner %1)
                          (-> (f/parallelize c [1 2 3 4 5 6 7 8 9 10] 1)
                              (f/map-to-pair (fn [x] (f/tuple x x)))
                              (f/partition-by (f/hash-partitioner 2))
                              (f/map-values #(* %1 2))
                              (f/partitioner)
                              )))


                    (testing
                      "partition-by partitions a given RDD according to the partitioning-fn using a hash partitioner."
                      (is (equals-ignore-order?
                            (-> (f/parallelize-pairs c
                                                     [#sparkling/tuple[{:a 1 :b 1} 11] ;; for me, (mod (hash 1) 2) and (mod (hash 3) 2) return 0 and 1 respectively, resulting in splitting the rdd in two partitions based upon :b
                                                      #sparkling/tuple[{:a 2 :b 1} 11]
                                                      #sparkling/tuple[{:a 3 :b 1} 12]
                                                      #sparkling/tuple[{:a 4 :b 3} 12]
                                                      #sparkling/tuple[{:a 5 :b 3} 13]] 1)
                                (f/partition-by (f/hash-partitioner
                                                  :b
                                                  2))
                                f/glom
                                f/collect
                                vec)
                            [; this is partition 1:
                             [#sparkling/tuple[{:a 1 :b 1} 11]
                              #sparkling/tuple[{:a 2 :b 1} 11]
                              #sparkling/tuple[{:a 3 :b 1} 12]]
                             ; and this is partition 2:
                             [#sparkling/tuple[{:a 4 :b 3} 12]
                              #sparkling/tuple[{:a 5 :b 3} 13]]]))

                      )


                    (let [b-partitioner (f/hash-partitioner
                                          :b
                                          2)
                          rdd (f/parallelize-pairs c
                                                   [#sparkling/tuple[{:a 1 :b 1} {:a 1 :b 1 :c 1}]
                                                    #sparkling/tuple[{:a 2 :b 1} {:a 2 :b 1 :c 2}]
                                                    #sparkling/tuple[{:a 3 :b 1} {:a 3 :b 1 :c 3}]
                                                    #sparkling/tuple[{:a 4 :b 3} {:a 4 :b 3 :c 4}]
                                                    #sparkling/tuple[{:a 5 :b 3} {:a 5 :b 3 :c 5}]] 1)
                          re-partitioned-rdd (->
                                               rdd
                                               (f/partition-by b-partitioner)
                                               (f/rekey-preserving-partitioning-without-check
                                                 (fd/tuple-fn
                                                   (fn [_ value] (f/tuple (select-keys value [:b :c]) value)))))]
                      (testing
                        "rekey keeps the hash partitioner if told to"
                        (is (= b-partitioner (f/partitioner re-partitioned-rdd))))

                      (testing
                        "rekey does keep all elements"
                        (is (= (f/collect re-partitioned-rdd)

                               [#sparkling/tuple[{:c 1 :b 1} {:a 1 :b 1 :c 1}]
                                #sparkling/tuple[{:c 2 :b 1} {:a 2 :b 1 :c 2}]
                                #sparkling/tuple[{:c 3 :b 1} {:a 3 :b 1 :c 3}]
                                #sparkling/tuple[{:c 4 :b 3} {:a 4 :b 3 :c 4}]
                                #sparkling/tuple[{:c 5 :b 3} {:a 5 :b 3 :c 5}]]
                               )))



                      (testing
                        "union concats two identical RDDs "
                        (is (equals-ignore-order? (-> (f/partitioner-aware-union re-partitioned-rdd re-partitioned-rdd)
                                                      f/collect
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
                        (is (= b-partitioner (f/partitioner (f/partitioner-aware-union re-partitioned-rdd re-partitioned-rdd)))))


                      #_(testing
                        "union keeps the hash partitioner if told to"
                        (is (= b-partitioner (f/partitioner
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
                        (let [rdd1 (f/parallelize c [1 2 3 4])
                              rdd2 (f/parallelize c [11 12 13])
                              rdd3 (f/parallelize c [21 22 23])]
                          (is (equals-ignore-order?
                                (-> (f/union rdd1 rdd2 rdd3)
                                    f/collect
                                    vec)
                                [1 2 3 4 11 12 13 21 22 23]))))

                      ))))

(deftest
  other-stuff

  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))
        kryo (ks/kryo-serializer)]
    (f/with-context c conf


                    (testing
                      "roundtrip composed fns"
                      (is (= ((ks/round-trip kryo (comp (partial * 2) inc)) 1) 4))
                      )
                    )))
