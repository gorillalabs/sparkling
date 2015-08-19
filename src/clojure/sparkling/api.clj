;; This is the deprecated entry point to sparkling, please use sparkling.core instead if you start a new project.

(ns sparkling.api
  (:refer-clojure :exclude [map reduce first count take distinct filter group-by values partition-by keys])
  (:require [sparkling.core :as sc])
  (:import [org.apache.spark.api.java JavaRDD JavaPairRDD]
           [org.apache.spark Partitioner]
           ))

(def STORAGE-LEVELS sc/STORAGE-LEVELS)


(def spark-context sc/spark-context)

(def local-spark-context sc/local-spark-context)

(defmacro with-context
  [context-sym conf & body]
  `(let [~context-sym (sparkling.core/spark-context ~conf)]
     (try
       ~@body
       (finally (sparkling.core/stop ~context-sym)))))

(def tuple sc/tuple)



(def text-file sc/text-file)

(defn parallelize
  "Distributes a local collection to form/return an RDD"
  ([spark-context lst] (sc/parallelize spark-context lst))
  ([spark-context lst num-slices] (sc/parallelize spark-context num-slices lst)))

(defn parallelize-pairs
  "Distributes a local collection to form/return an RDD"
  ([spark-context lst] (sc/parallelize-pairs spark-context lst))
  ([spark-context lst num-slices] (sc/parallelize-pairs spark-context num-slices lst)))

(def union sc/union)

(def partitioner-aware-union sc/partitioner-aware-union)

(defn partitionwise-sampled-rdd
  "Creates a PartitionwiseSampledRRD from existing RDD and a sampler object"
  [rdd sampler preserve-partitioning? seed]
  (sc/partitionwise-sampled-rdd sampler preserve-partitioning? seed rdd))

;; ## Transformations
;;
;; Function for transforming RDDs
;;
(defn map
  "Returns a new RDD formed by passing each element of the source through the function `f`."
  [rdd f]
  (sc/map f rdd))

(defn map-to-pair
  "Returns a new `JavaPairRDD` of (K, V) pairs by applying `f` to all elements of `rdd`."
  [rdd f]
  (sc/map-to-pair f rdd ))

(defn map-values [rdd f]
  (sc/map-values f rdd))

(defn reduce
  "Aggregates the elements of `rdd` using the function `f` (which takes two arguments
  and returns one). The function should be commutative and associative so that it can be
  computed correctly in parallel."
  [rdd f]
  (sc/reduce f rdd))

(defn values
  "Returns the values of a JavaPairRDD"
  [rdd]
  (sc/values rdd))

(defn flat-map
  "Similar to `map`, but each input item can be mapped to 0 or more output items (so the
   function `f` should return a collection rather than a single item)"
  [rdd f]
  (sc/flat-map f rdd))

(defn flat-map-to-pair
  "Returns a new `JavaPairRDD` by first applying `f` to all elements of `rdd`, and then flattening
  the results."
  [rdd f]
  (sc/flat-map-to-pair f rdd))

(defn flat-map-values
  [rdd f]
  (sc/flat-map-values f rdd))

(defn map-partition
  "Similar to `map`, but runs separately on each partition (block) of the `rdd`, so function `f`
  must be of type Iterator<T> => Iterable<U>.
  https://issues.apache.org/jira/browse/SPARK-3369"
  [rdd f]
  (sc/map-partition f rdd))


(defn map-partitions-to-pair
  "Similar to `map`, but runs separately on each partition (block) of the `rdd`, so function `f`
  must be of type Iterator<T> => Iterable<U>.
  https://issues.apache.org/jira/browse/SPARK-3369"
  [rdd f & {:keys [preserves-partitioning]}]
  (sc/map-partitions-to-pair f preserves-partitioning rdd))

(defn map-partition-with-index
  "Similar to `map-partition` but function `f` is of type (Int, Iterator<T>) => Iterator<U> where
  `i` represents the index of partition."
  [rdd f]
  (sc/map-partition-with-index f rdd))

(defn filter
  "Returns a new RDD containing only the elements of `rdd` that satisfy a predicate `f`."
  [rdd f]
  (sc/filter f rdd))

(defn foreach
  "Applies the function `f` to all elements of `rdd`."
  [rdd f]
  (sc/foreach f rdd))

(defn aggregate
  "Aggregates the elements of each partition, and then the results for all the partitions,
   using a given combine function and a neutral 'zero value'."
  [rdd zero-value seq-op comb-op]
  (sc/aggregate seq-op comb-op zero-value rdd ))

(defn fold
  "Aggregates the elements of each partition, and then the results for all the partitions,
  using a given associative function and a neutral 'zero value'"
  [rdd zero-value f]
  (sc/fold f zero-value rdd ))


(defn reduce-by-key
  "When called on an `rdd` of (K, V) pairs, returns an RDD of (K, V) pairs
  where the values for each key are aggregated using the given reduce function `f`."
  [rdd f]
  (sc/reduce-by-key f rdd))

(defn cartesian
  "Creates the cartesian product of two RDDs returning an RDD of pairs"
  [rdd1 rdd2]
  (sc/cartesian rdd1 rdd2))

(defn intersection
  [rdd1 rdd2]
  (sc/intersection rdd1 rdd2))

(defn subtract
  "Removes all elements from rdd1 that are present in rdd2."
  [rdd1 rdd2]
  (sc/subtract rdd1 rdd2))

(defn subtract-by-key
  "Return each (key, value) pair in rdd1 that has no pair with matching key in rdd2."
  [rdd1 rdd2]
  (sc/subtract-by-key rdd1 rdd2))

(defn group-by
  "Returns an RDD of items grouped by the return value of function `f`."
  ([rdd f]
    (sc/group-by f rdd))
  ([rdd f n]
    (sc/group-by f n rdd)))

(defn group-by-key
  "Groups the values for each key in `rdd` into a single sequence."
  ([rdd]
    (sc/group-by-key rdd))
  ([rdd n]
    (sc/group-by-key n rdd)))

(defn combine-by-key
  "Combines the elements for each key using a custom set of aggregation functions.
  Turns an RDD of (K, V) pairs into a result of type (K, C), for a 'combined type' C.
  Note that V and C can be different -- for example, one might group an RDD of type
  (Int, Int) into an RDD of type (Int, List[Int]).
  Users must provide three functions:
  -- createCombiner, which turns a V into a C (e.g., creates a one-element list)
  -- mergeValue, to merge a V into a C (e.g., adds it to the end of a list)
  -- mergeCombiners, to combine two C's into a single one."
  ([rdd create-combiner merge-value merge-combiners]
    (sc/combine-by-key
                   create-combiner
                   merge-value
                   merge-combiners
                   rdd))
  ([rdd create-combiner merge-value merge-combiners n]
    (sc/combine-by-key
                   create-combiner
                   merge-value
                   merge-combiners
                   n
                   rdd)))

(defn sort-by-key
  "When called on `rdd` of (K, V) pairs where K implements ordered, returns a dataset of
   (K, V) pairs sorted by keys in ascending or descending order, as specified by the boolean
   ascending argument."
  ([rdd]
    (sc/sort-by-key rdd))
  ([rdd x]
      (sc/sort-by-key x rdd))
  ([rdd compare-fn asc?]
    (sc/sort-by-key compare-fn asc? rdd)))

(defn join
  "When called on `rdd` of type (K, V) and (K, W), returns a dataset of
  (K, (V, W)) pairs with all pairs of elements for each key."
  [rdd other]
  (sc/join rdd other))

(defn left-outer-join
  "Performs a left outer join of `rdd` and `other`. For each element (K, V)
   in the RDD, the resulting RDD will either contain all pairs (K, (V, W)) for W in other,
   or the pair (K, (V, nil)) if no elements in other have key K."
  [rdd other]
  (sc/left-outer-join rdd other))

(defn sample
  "Returns a `fraction` sample of `rdd`, with or without replacement,
  using a given random number generator `seed`."
  [rdd with-replacement? fraction seed]
  (sc/sample with-replacement? fraction seed rdd))

(defn coalesce
  "Decrease the number of partitions in `rdd` to `n`.
  Useful for running operations more efficiently after filtering down a large dataset."
  ([rdd n]
    (sc/coalesce n rdd))
  ([rdd n shuffle?]
    (sc/coalesce n shuffle? rdd)))

(def count-partitions sc/count-partitions)

(defn coalesce-max
  "Decrease the number of partitions in `rdd` to `n`.
  Useful for running operations more efficiently after filtering down a large dataset."
  ([rdd n]
    (sc/coalesce n rdd))
  ([rdd n shuffle?]
    (sc/coalesce n shuffle? rdd)))

(defn repartition
  "Returns a new `rdd` with exactly `n` partitions."
  [rdd n]
  (sc/repartition n rdd))

;; ## Actions
;;
;; Action return their results to the driver process.
;;
(def count-by-key sc/count-by-key)

(def count-by-value sc/count-by-value)

(defn save-as-text-file
  "Writes the elements of `rdd` as a text file (or set of text files)
  in a given directory `path` in the local filesystem, HDFS or any other Hadoop-supported
  file system. Spark will call toString on each element to convert it to a line of
  text in the file."
  [rdd path]
  (sc/save-as-text-file path rdd))


(defn persist
  "Sets the storage level of `rdd` to persist its values across operations
  after the first time it is computed. storage levels are available in the `STORAGE-LEVELS' map.
  This can only be used to assign a new storage level if the RDD does not have a storage level set already."
  [rdd storage-level]
  (sc/storage-level! storage-level rdd))

(def first
  sc/first)

(def count
  sc/count)

(def glom
  "Returns an RDD created by coalescing all elements of `rdd` within each partition into a list."
  sc/glom)

(def cache
  "Persists `rdd` with the default storage level (`MEMORY_ONLY`)."
  sc/cache)

(def lookup
  "Return the vector of values in the RDD for key `key`. Your key has to be serializable with the Java serializer (not Kryo like usual) to use this."
  sc/lookup)


(def collect
  "Returns all the elements of `rdd` as an array at the driver process."
  sc/collect)

(def collect-map
  "Retuns all elements of `pair-rdd` as a map at the driver process.
  Attention: The resulting map will only have one entry per key.
             Thus, if you have multiple tuples with the same key in the pair-rdd, the collection returned will not contain all elements!
             The function itself will *not* issue a warning of any kind!"
  sc/collect-map)


(defn distinct
  "Return a new RDD that contains the distinct elements of the source `rdd`."
  ([rdd]
    (sc/distinct rdd))
  ([rdd n]
    (sc/distinct n rdd)))

(defn take
  "Return an array with the first n elements of `rdd`.
  (Note: this is currently not executed in parallel. Instead, the driver
  program computes all the elements)."
  [rdd cnt]
  (sc/take cnt rdd))

(def partitions sc/partitions)


(defn hash-partitioner
  ([n]
    (sc/hash-partitioner n))
  ([subkey-fn n]
    (sc/hash-partitioner subkey-fn n)))

(defn partition-by
  [^JavaPairRDD rdd ^Partitioner partitioner]
  (sc/partition-by partitioner rdd))

(defn foreach-partition
  "Applies the function `f` to all elements of `rdd`."
  [rdd f]
  (sc/foreach-partition f rdd))


(defn key-by
  "Creates tuples of the elements in this RDD by applying `f`."
  [^JavaRDD rdd f]
  (sc/key-by f rdd))

(def keys sc/keys)



(def cogroup sc/cogroup)


(def checkpoint  sc/checkpoint)

(defn rdd-name
  ([rdd name]
    (sc/rdd-name name rdd))
  ([rdd]
    (sc/rdd-name rdd)))



(defn histogram [rdd buckets]
  (sc/histogram buckets rdd))

(def partitioner  sc/partitioner)

(defn rekey-preserving-partitioning-without-check
  "This re-keys a pair-rdd by applying the rekey-fn to generate new tuples. However, it does not check whether your new keys would keep the same partitioning, so watch out!!!!"
  [rdd rekey-fn]
  (sc/rekey rekey-fn rdd))

