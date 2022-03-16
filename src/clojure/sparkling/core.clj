(ns sparkling.core
  "This is the main entry point to sparkling, typically required like `[sparkling.core :as s]`.

  By design, most operations in sparkling are built up via the thread-last macro `->>`.
  Thus, they work exactly like their clojure.core counterparts, making it easy to migrate Clojure-only code to Spark code.

  If you find an RDD operation missing from the api that you'd like to use, pull requests are
  happily accepted!"

  (:refer-clojure :exclude [map reduce first count take distinct filter group-by values partition-by keys max min])
  (:require [clojure.tools.logging :as log]
            [sparkling.destructuring :as ds]
            [sparkling.function :refer [flat-map-function
                                        flat-map-function2
                                        function
                                        function2
                                        function3
                                        pair-function
                                        pair-flat-map-function
                                        void-function]]
            [sparkling.scalaInterop :as si]
            [sparkling.conf :as conf]
            [sparkling.utils :as u]
            [sparkling.kryo :as k]
            [sparkling.destructuring :as s-de])
  (:import [scala Tuple2]
           [java.util Comparator ArrayList]
           [org.apache.spark.api.java JavaSparkContext StorageLevels
                                      JavaRDD JavaPairRDD JavaDoubleRDD]
           [org.apache.spark HashPartitioner Partitioner]
           [org.apache.spark.sql SparkSession]
           [org.apache.spark.rdd PartitionwiseSampledRDD PartitionerAwareUnionRDD]
           [scala.collection JavaConverters]
           [scala.reflect ClassTag$]))


;; sparkling makes extensive use of kryo to serialize and deserialize clojure functions
;; and data structures. Here we ensure that these properties are set so they are inhereted
;; into any `SparkConf` objects that are created.
;;
;; sparkling WILL NOT WORK without enabling kryo serialization in spark!
;;
;(System/setProperty "spark.serializer" "org.apache.spark.serializer.KryoSerializer")
;(System/setProperty "spark.kryo.registrator" "sparkling.serialization.BaseRegistrator")

(def STORAGE-LEVELS {:memory-only           StorageLevels/MEMORY_ONLY
                     :memory-only-ser       StorageLevels/MEMORY_ONLY_SER
                     :memory-and-disk       StorageLevels/MEMORY_AND_DISK
                     :memory-and-disk-ser   StorageLevels/MEMORY_AND_DISK_SER
                     :disk-only             StorageLevels/DISK_ONLY
                     :memory-only-2         StorageLevels/MEMORY_ONLY_2
                     :memory-only-ser-2     StorageLevels/MEMORY_ONLY_SER_2
                     :memory-and-disk-2     StorageLevels/MEMORY_AND_DISK_2
                     :memory-and-disk-ser-2 StorageLevels/MEMORY_AND_DISK_SER_2
                     :disk-only-2           StorageLevels/DISK_ONLY_2
                     :none                  StorageLevels/NONE})



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Utils
(declare count-partitions)

(defn jar-of-ns
  [ns]
  (let [clazz (Class/forName (clojure.string/replace (str ns) #"-" "_"))]
    (JavaSparkContext/jarOfClass clazz)))

(def tuple s-de/tuple)

(defn- ftruthy?
  [f]
  (fn [x] (u/truthy? (f x))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Cluster and context info
;;
;; Functions which provides Cluster and context info
;;

(defn default-min-partitions
  "Default min number of partitions for Hadoop RDDs when not given by user"
  [spark-context]
  (.defaultMinPartitions spark-context))


(defn default-parallelism
  "Default level of parallelism to use when not given by user (e.g. parallelize and makeRDD)."
  [spark-context]
  (.defaultParallelism spark-context))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; RDD construction
;;
;; Functions for constructing new RDDs
;;
(defn text-file
  "Reads a text file from HDFS, a local file system (available on all nodes),
  or any Hadoop-supported file system URI, and returns it as an `JavaRDD` of Strings."
  ([spark-context filename]
  (.textFile spark-context filename))
  ([spark-context filename min-partitions]
   (.textFile spark-context filename min-partitions)))

(defn whole-text-files
  "Read a directory of text files from HDFS, a local file system (available on all nodes),
  or any Hadoop-supported file system URI. Each file is read as a single record and returned 
  in a key-value pair, where the key is the path of each file, the value is the content of each file."
  ([spark-context filename min-partitions]
   (.wholeTextFiles spark-context filename min-partitions))
  ([spark-context filename]
   (.wholeTextFiles spark-context filename)))

(defn into-rdd
  "Distributes a local collection to form/return an RDD"
  ([spark-context lst]
   (u/set-auto-name
     (.parallelize spark-context lst)))
  ([spark-context num-slices lst]
   (u/set-auto-name
     (.parallelize spark-context lst num-slices)
     num-slices)))

(defn into-pair-rdd
  "Distributes a local collection to form/return an RDD"
  ([spark-context lst] (u/set-auto-name (.parallelizePairs spark-context lst)))
  ([spark-context num-slices lst] (u/set-auto-name (.parallelizePairs spark-context lst num-slices) num-slices)))

(def parallelize into-rdd)
(def parallelize-pairs into-pair-rdd)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Transformations
;; Functions for transforming RDDs
;;

;; functions on one rdd
(defn map
  "Returns a new RDD formed by passing each element of the source through the function `f`."
  [f rdd]
  (-> (.map rdd (function f))
      (u/set-auto-name (u/unmangle-fn f))))

(defn map-to-pair
  "Returns a new `JavaPairRDD` of (K, V) pairs by applying `f` to all elements of `rdd`."
  [f rdd]
  (-> (.mapToPair rdd (pair-function f))
      (u/set-auto-name (u/unmangle-fn f))))

(defn map-values [f rdd]
  "Returns a new `JavaPairRDD` of (K,V) pairs by applying `f` to all values in the 
  pair-rdd `rdd`"  
  (-> (.mapValues rdd (function f))
      (u/set-auto-name (u/unmangle-fn f))))

(defn reduce
  "Aggregates the elements of `rdd` using the function `f` (which takes two arguments
  and returns one). The function should be commutative and associative so that it can be
  computed correctly in parallel."
  [f rdd]
  (u/set-auto-name (.reduce rdd (function2 f)) (u/unmangle-fn f)))

(defn values
  "Returns the values of a JavaPairRDD"
  [rdd]
  (u/set-auto-name (.values rdd)))

(defn flat-map
  "Similar to `map`, but each input item can be mapped to 0 or more output items (so the
   function `f` should return a collection rather than a single item)"
  [f rdd]
  (u/set-auto-name
    (.flatMap rdd (flat-map-function f))
    (u/unmangle-fn f)))

(defn flat-map-to-pair
  "Returns a new `JavaPairRDD` by first applying `f` to all elements of `rdd`, and then flattening
  the results."
  [f rdd]
  (u/set-auto-name
    (.flatMapToPair rdd (pair-flat-map-function f))
    (u/unmangle-fn f)))

(defn flat-map-values
  "Returns a `JavaPairRDD` by applying `f` to all values of `rdd`, and then
  flattening the results"
  [f rdd]
  (u/set-auto-name
    (.flatMapValues rdd (flat-map-function f))
    (u/unmangle-fn f)))

(defn map-partition
  "Similar to `map`, but runs separately on each partition (block) of the `rdd`, so function `f`
  must be of type Iterator<T> => Iterable<U>.
  https://issues.apache.org/jira/browse/SPARK-3369"
  [f rdd]
  (u/set-auto-name
    (.mapPartitions rdd (flat-map-function f))
    (u/unmangle-fn f)))


(defn map-partitions-to-pair
  "Similar to `map`, but runs separately on each partition (block) of the `rdd`, so function `f`
  must be of type Iterator<T> => Iterable<U>.
  https://issues.apache.org/jira/browse/SPARK-3369"
  [f preserve-partitioning? rdd]
  (u/set-auto-name
    (.mapPartitionsToPair rdd (pair-flat-map-function f) (u/truthy? preserve-partitioning?))
    (u/unmangle-fn f)
    preserve-partitioning?))

(defn map-partition-with-index
  "Similar to `map-partition` but function `f` is of type (Int, Iterator<T>) => Iterator<U> where
  `i` represents the index of partition."
  [f rdd]
  (u/set-auto-name
    (.mapPartitionsWithIndex rdd (function2 f) true)
    (u/unmangle-fn f)))



(defn filter
  "Returns a new RDD containing only the elements of `rdd` that satisfy a predicate `f`."
  [f rdd]
  (u/set-auto-name
    (.filter rdd (function (ftruthy? f)))
    (u/unmangle-fn f)))





(defn foreach
  "Applies the function `f` to all elements of `rdd`."
  [f rdd]
  (.foreach rdd (void-function f)))

(defn aggregate
  "Aggregates the elements of each partition, and then the results for all the partitions,
   using a given combine function and a neutral 'zero value'."
  [item-to-seq-fn combine-seqs-fn zero-value rdd]
  (.aggregate rdd zero-value (function2 item-to-seq-fn) (function2 combine-seqs-fn)))

(defn fold
  "Aggregates the elements of each partition, and then the results for all the partitions,
  using a given associative function and a neutral 'zero value'"
  [f zero-value rdd]
  (.fold rdd zero-value (function2 f)))

(defn tuple-by [f]
  (fn tuple-by-fn [x]
    (tuple (f x) x)))

(defn key-by
  "Creates tuples of the elements in this RDD by applying `f`."
  [f ^JavaRDD rdd]
  (u/set-auto-name
    (map-to-pair (tuple-by f) rdd)
    (u/unmangle-fn f)))

(defn keys
  "Return an RDD with the keys of each tuple."
  [^JavaPairRDD rdd]
  (u/set-auto-name
    (.keys rdd)))


(defn reduce-by-key
  "When called on an `rdd` of (K, V) pairs, returns an RDD of (K, V) pairs
  where the values for each key are aggregated using the given reduce function `f`."
  [f rdd]
  (u/set-auto-name
    (.reduceByKey rdd (function2 f))
    (u/unmangle-fn f)))

(defn group-by
  "Returns an RDD of items grouped by the return value of function `f`."
  ([f rdd]
   (u/set-auto-name
     (.groupBy rdd (function f))
     (u/unmangle-fn f)))
  ([f n rdd]
   (u/set-auto-name
     (.groupBy rdd (function f) n)
     (u/unmangle-fn f)
     n)))

(defn group-by-key
  "Groups the values for each key in `rdd` into a single sequence."
  ([rdd]
   (u/set-auto-name
     (.groupByKey rdd)))
  ([n rdd]
   (u/set-auto-name
     (.groupByKey rdd n)
     n)))

(defn combine-by-key
  "Combines the elements for each key using a custom set of aggregation functions.
  Turns an RDD of (K, V) pairs into a result of type (K, C), for a 'combined type' C.
  Note that V and C can be different -- for example, one might group an RDD of type
  (Int, Int) into an RDD of type (Int, List[Int]).
  Users must provide three functions:
  -- seq-fn, which turns a V into a C (e.g., creates a one-element list)
  -- conj-fn, to merge a V into a C (e.g., adds it to the end of a list)
  -- merge-fn, to combine two C's into a single one."
  ([seq-fn conj-fn merge-fn rdd]
   (u/set-auto-name
     (.combineByKey rdd
                    (function seq-fn)
                    (function2 conj-fn)
                    (function2 merge-fn))
     (u/unmangle-fn seq-fn)
     (u/unmangle-fn conj-fn)
     (u/unmangle-fn merge-fn)))
  ([seq-fn conj-fn merge-fn n rdd]
   (u/set-auto-name
     (.combineByKey rdd
                    (function seq-fn)
                    (function2 conj-fn)
                    (function2 merge-fn)
                    n)
     (u/unmangle-fn seq-fn)
     (u/unmangle-fn conj-fn)
     (u/unmangle-fn merge-fn)
     n
     )))

(defn- wrap-comparator [f]
  "Turn a plain function into a Comparator instance, or pass a Comparator unchanged"
  (if (instance? Comparator f) f (comparator f)))

(defn sort-by-key
  "When called on `rdd` of (K, V) pairs where K implements ordered, returns a dataset of
   (K, V) pairs sorted by keys in ascending or descending order, as specified by the boolean
   ascending argument."
  ([rdd]
   (u/set-auto-name
     (sort-by-key compare true rdd)))
  ([x rdd]
    ;; RDD has a .sortByKey signature with just a Boolean arg, but it doesn't
    ;; seem to work when I try it, bool is ignored.
   (if (instance? Boolean x)
     (sort-by-key compare x rdd)
     (sort-by-key x true rdd)))
  ([compare-fn asc? rdd]
   (u/set-auto-name
     (.sortByKey rdd
                 (wrap-comparator compare-fn)
                 (u/truthy? asc?))
     (u/unmangle-fn compare-fn)
     asc?
     )))

(defn max
  "Return the maximum value in `rdd` in the ordering defined by `compare-fn`"
  [compare-fn rdd]
  (u/set-auto-name
   (.max rdd (wrap-comparator compare-fn))
   (u/unmangle-fn compare-fn)))

(defn min
  "Return the minimum value in `rdd` in the ordering defined by `compare-fn`"
  [compare-fn rdd]
  (u/set-auto-name
   (.min rdd (wrap-comparator compare-fn))
   (u/unmangle-fn compare-fn)))

(defn sample
  "Returns a `fraction` sample of `rdd`, with or without replacement,
  using a given random number generator `seed`."
  [with-replacement? fraction seed rdd]
  (u/set-auto-name
    (.sample rdd with-replacement? fraction seed)
    with-replacement? fraction seed))

(defn partitionwise-sampled-rdd
  "Creates a PartitionwiseSampledRRD from existing RDD and a sampler object"
  [sampler preserve-partitioning? seed rdd]
  (-> (PartitionwiseSampledRDD.
        (.rdd rdd)
        sampler
        preserve-partitioning?
        seed
        si/OBJECT-CLASS-TAG
        si/OBJECT-CLASS-TAG)
      (JavaRDD/fromRDD si/OBJECT-CLASS-TAG)
      (u/set-auto-name)
      ))

(defn coalesce
  "Decrease the number of partitions in `rdd` to `n`.
  Useful for running operations more efficiently after filtering down a large dataset."
  ([n rdd]
   (u/set-auto-name
     (.coalesce rdd n)
     n))
  ([n shuffle? rdd]
   (u/set-auto-name
     (.coalesce rdd n shuffle?)
     n shuffle?)))


(defn coalesce-max
  "Decrease the number of partitions in `rdd` to `n`.
  Useful for running operations more efficiently after filtering down a large dataset."
  ([n rdd]
   (u/set-auto-name
     (.coalesce rdd (clojure.core/min n (count-partitions rdd))) n))
  ([n shuffle? rdd]
   (u/set-auto-name
     (.coalesce rdd (clojure.core/min n (count-partitions rdd)) shuffle?) n shuffle?)))


(defn zip-with-index
  "Zips this RDD with its element indices, creating an RDD of tuples of (item, index)"
  [rdd]
  (u/set-auto-name (.zipWithIndex rdd)))


(defn zip-with-unique-id
  "Zips this RDD with generated unique Long ids, creating an RDD of tuples of (item, uniqueId)"
  [rdd]
  (u/set-auto-name (.zipWithUniqueId rdd)))


;; functions on multiple rdds

(defn cogroup
  ([^JavaPairRDD rdd ^JavaPairRDD other]
   (u/set-auto-name
     (.cogroup rdd other)))
  ([^JavaPairRDD rdd ^JavaPairRDD other1 ^JavaPairRDD other2]
   (u/set-auto-name
     (.cogroup rdd
               other1
               other2)))
  ([^JavaPairRDD rdd ^JavaPairRDD other1 ^JavaPairRDD other2 ^JavaPairRDD other3]
   (u/set-auto-name
     (.cogroup rdd
               other1
               other2
               other3))))

(defn union
  "Build the union of two or more RDDs"
  ([rdd1 rdd2]
   (u/set-auto-name (.union rdd1 rdd2)))
  ([rdd1 rdd2 & rdds]
   (u/set-auto-name (.union (JavaSparkContext/fromSparkContext (.context rdd1))
                            (into-array JavaRDD (concat [rdd1 rdd2] rdds))))))

(defn partitioner-aware-union [pair-rdd1 pair-rdd2 & pair-rdds]
  ;; TODO: add check to make sure every rdd is a pair-rdd and has the same partitioner.
  (u/set-auto-name
    (JavaPairRDD/fromRDD
      (PartitionerAwareUnionRDD.
        (.context pair-rdd1)
        (.toSeq
         (JavaConverters/asScalaBuffer (clojure.core/mapv #(.rdd %1) (conj pair-rdds pair-rdd2 pair-rdd1))))
        si/OBJECT-CLASS-TAG
        )
      si/OBJECT-CLASS-TAG
      si/OBJECT-CLASS-TAG
      )))

(defn join
  "When called on `rdd` of type (K, V) and (K, W), returns a dataset of
  (K, (V, W)) pairs with all pairs of elements for each key."
  [rdd other]
  (u/set-auto-name
    (.join rdd other)))

(defn left-outer-join
  "Performs a left outer join of `rdd` and `other`. For each element (K, V)
   in the RDD, the resulting RDD will either contain all pairs (K, (V, W)) for W in other,
   or the pair (K, (V, nil)) if no elements in other have key K."
  [rdd other]
  (u/set-auto-name
    (.leftOuterJoin rdd other)))


(defn key-by-fn "Wraps a function f to be called with the value v of a tuple from spark,
so that the wrapped function returns a tuple [f(v),v]"
  [f]
  (fn [^Tuple2 t]
    (let [v (ds/value t)]
      (tuple (f v) v))))

(declare rekey)

(defn intersect-by-key
  "Intersects rdd1 with rdd2 by key,
  i.e. rdd1 is rekeyed by keyfn,
  then joined to keep only those elements with keys in rdd2
  and rekeyed again with keybackfn to bring back the original structure.

  Remember, rekey is performed by partition,
  thus keyfn and keybackfn and the original partitioning should work with the given partitioner."
  [rdd1 keyfn keybackfn rdd2]
  (->>
    rdd1
    (rekey (key-by-fn keyfn))
    (join rdd2)
    (map-values (ds/second-value-fn identity))
    (rekey (key-by-fn keybackfn))
    (u/set-auto-name)))


(defn cartesian
  "Creates the cartesian product of two RDDs returning an RDD of pairs"
  [rdd1 rdd2]
  (u/set-auto-name
    (.cartesian rdd1 rdd2)))

(defn intersection
  [rdd1 rdd2]
  (u/set-auto-name
   (.intersection rdd1 rdd2)))

(defn subtract
  "Removes all elements from rdd1 that are present in rdd2."
  [rdd1 rdd2]
  (u/set-auto-name
   (.subtract rdd1 rdd2)))

(defn subtract-by-key
  "Return each (key, value) pair in rdd1 that has no pair with matching key in rdd2."
  [rdd1 rdd2]
  (u/set-auto-name
   (.subtractByKey rdd1 rdd2)))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Actions
;;
;; Action return their results to the driver process.
;;

(defn count-by-key
  "Only available on RDDs of type (K, V).
  Returns a map of (K, Int) pairs with the count of each key."
  [rdd]
  (into {}
        (.countByKey rdd)))

(defn count-by-value
  "Return the count of each unique value in `rdd` as a map of (value, count)
  pairs."
  [rdd]
  (into {} (.countByValue rdd)))


(def first
  "Returns the first element of `rdd`."
  (memfn first))

(def count
  "Return the number of elements in `rdd`."
  (memfn count))

(def glom
  "Returns an RDD created by coalescing all elements of `rdd` within each partition into a list."
  (memfn glom))

(defn lookup [pair-rdd key]
  "Return the vector of values in the RDD for key `key`. Your key has to be serializable with the Java serializer (not Kryo like usual) to use this."
  (vec (.lookup pair-rdd key)))

(def collect
  "Returns all the elements of `rdd` as an array at the driver process."
  (memfn collect))

(defn collect-map
  "Retuns all elements of `pair-rdd` as a map at the driver process.
  Attention: The resulting map will only have one entry per key.
             Thus, if you have multiple tuples with the same key in the pair-rdd, the collection returned will not contain all elements!
             The function itself will *not* issue a warning of any kind!"
  [pair-rdd]
  (persistent!
    (clojure.core/reduce
    (fn [coll ^Tuple2 t]
      (assoc! coll (s-de/key t) (s-de/value t)))
    (transient {})
    (collect pair-rdd))))


(defn distinct
  "Return a new RDD that contains the distinct elements of the source `rdd`."
  ([rdd]
   (.distinct rdd))
  ([n rdd]
   (.distinct rdd n)))

(defn take
  "Return an array with the first n elements of `rdd`.
  (Note: this is currently not executed in parallel. Instead, the driver
  program computes all the elements)."
  [cnt rdd]
  (.take rdd cnt))


(defn foreach-partition
  "Applies the function `f` to all elements of `rdd`."
  [f rdd]
  (.foreachPartition rdd (void-function (comp f iterator-seq))))


(defn save-as-text-file
  "Writes the elements of `rdd` as a text file (or set of text files)
  in a given directory `path` in the local filesystem, HDFS or any other Hadoop-supported
  file system. Supports an optional codec class like org.apache.hadoop.io.compress.GzipCodec.
  Spark will call toString on each element to convert it to a line of
  text in the file."
  ([path rdd]
    (.saveAsTextFile rdd path))
  ([path rdd codec-class]
    (.saveAsTextFile rdd path codec-class)))


;; Histogram related functions


(defmulti histogram "compute histogram of an RDD of doubles"
          (fn [bucket-arg _] (sequential? bucket-arg)))

(defmethod histogram true [buckets rdd]
  (let [counts (-> (JavaDoubleRDD/fromRDD (.rdd rdd))
                   (.histogram (double-array buckets)))]
    (vec counts)))

(defmethod histogram false
  [bucket-count rdd]
  (let [^Tuple2 buckets-counts-tuple (-> (JavaDoubleRDD/fromRDD (.rdd rdd))
                                         (.histogram bucket-count))]
    [(vec (._1 buckets-counts-tuple)) (vec (._2 buckets-counts-tuple))]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Partitions and Partitioner-related stuff

(defn partitions
  "Returns a vector of partitions for a given JavaRDD"
  [javaRdd]
  (into [] (.partitions (.rdd javaRdd))))



(defn hash-partitioner
  ([n]
   (HashPartitioner. n))
  ([subkey-fn n]
   (proxy [HashPartitioner] [n]
     (getPartition [key]
       (let [subkey (subkey-fn key)]
         (mod (hash subkey) n))))))

(defn partition-by
  [^Partitioner partitioner ^JavaPairRDD rdd]
  (.partitionBy rdd partitioner))

(defn partitioner [^JavaPairRDD rdd]
  (si/some-or-nil (.partitioner (.rdd rdd))))

(defn count-partitions [rdd]
  (alength (.partitions (.rdd rdd))))

(defn repartition
  "Returns a new `rdd` with exactly `n` partitions."
  [n rdd]
  (u/set-auto-name
    (.repartition rdd n)
    n))


(defn rekey
  "This re-keys a pair-rdd by applying the rekey-fn to generate new tuples.
  However, it does not check whether your new keys would keep the same partitioning, so watch out!!!!"
  [rekey-fn rdd]
  (u/set-auto-name
    (map-partitions-to-pair
      (comp (partial clojure.core/map rekey-fn) iterator-seq)
      true
      rdd)
    (u/unmangle-fn rekey-fn)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; RDD management


(def cache
  "Persists `rdd` with the default storage level (`MEMORY_ONLY`)."
  (memfn cache))

(defn uncache
  "Marks `rdd` as non-persistent (removes all blocks for it from memory and disk).  If `blocking?` is true, block until the operation is complete."
  ([blocking? rdd] (.unpersist rdd blocking?))
  ([rdd] (uncache false rdd)))

(defn storage-level!
  "Sets the storage level of `rdd` to persist its values across operations
  after the first time it is computed. storage levels are available in the `STORAGE-LEVELS' map.
  This can only be used to assign a new storage level if the RDD does not have a storage level set already."
  [storage-level rdd]
  (.persist rdd storage-level))


;; Add documentation, make sure you have checkpoint-directory set. How?
(defn checkpoint [rdd]
  (.checkpoint rdd))


(defn rdd-name
  ([name rdd]
   (.setName rdd name))
  ([rdd]
   (.name rdd)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;
;; Spark Context handling

(defn spark-context
  "Creates a spark context that loads settings from given configuration object
   or system properties"
  ([conf]
   (log/debug "JavaSparkContext" (conf/to-string conf))
   (JavaSparkContext. conf))
  ([master app-name]
   (log/debug "JavaSparkContext" master app-name)
   (let [conf (-> (conf/spark-conf)
                  (conf/master master)
                  (conf/app-name app-name))]
     (spark-context conf))))

(defn spark-session
  [^JavaSparkContext context]
  (SparkSession. (.sc context)))

(defn local-spark-context
  [app-name]
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name app-name))]
    (spark-context conf)))

(def stop
  (memfn #^JavaSparkContext stop))


(defmacro with-context
  [context-sym conf & body]
  `(let [~context-sym (sparkling.core/spark-context ~conf)]
     (try
       ~@body
       (finally (stop ~context-sym)))))
