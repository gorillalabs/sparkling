---
title: "Getting Started with Sparkling"
layout: article
---


## About this guide

This guide combines an overview of Sparkling with a quick tutorial that helps you to get started with it.
It should take about 10 minutes to read and study the provided code examples. This guide covers:

 * Feature of Sparkling, why Sparkling was created
 * Clojure and Apache Spark version requirements
 * How to add Sparkling dependency to your project


This work is licensed under a <a rel="license"
href="http://creativecommons.org/licenses/by/3.0/">Creative Commons
Attribution 3.0 Unported License</a> (including images &
stylesheets). The source is available [on
Github](https://github.com/gorillalabs/sparkling/tree/gh-pages).




## Sparkling Overview

Sparkling is an idiomatic Clojure wrapper around Apache Spark Scala/Java API. It
offers broad support of Spark transformations and actions, together with some additional things not available out of the box in other libraries, has minimal performance overhead and is well
maintained. However, it does not cover SparkSQL or Spark Streaming at the moment.

## Do I need a cluster? Do I need to install Apache Spark? Where do I start?
Apache Spark is a framework to run your code on a cluster. However, for tests and non-productive workload, you can run your code locally very easily. So, for your first steps, no further install is necessary, Sparkling comes batteries included.

## Example project
There is an example included in Sparkling, just check out [sparkling/example/tfidf.clj](https://github.com/gorillalabs/sparkling/blob/develop/example/sparkling/example/tfidf.clj).


## What Sparkling is not

With Sparkling, you still need to understand Apache Spark model of RDDs, transformations and actions, etc. Also, you should pay close attention to the way you model your data for optimized performance.
This is up to you and not provided by Sparkling. We only provide you with Canvas and brushes. It's up to you to learn how to paint.


## What version of Sparkling, Clojure, Spark does this guide cover?

 * This guide covers Sparkling 1.0.x (including preview releases).
 * Sparkling requires Clojure 1.6. The most recent stable release is highly
recommended.
 *Sparkling targets Apache Spark 1.1.x. Other versions of Spark might work, but not necessarily. Please note that some
features may be specific to recent Spark releases.


## Adding Sparkling Dependency To Your Project

Sparkling artifacts are [released to Clojars](https://clojars.org/gorillalabs/sparkling).

### With Leiningen

    [gorillalabs/sparkling "1.0.0-SNAPSHOT"]

### With Maven

Add Clojars repository definition to your `pom.xml`:

     <repository>
       <id>clojars.org</id>
       <url>http://clojars.org/repo</url>
     </repository>

And then the dependency:

    <dependency>
      <groupId>gorillalabs</groupId>
      <artifactId>sparkling</artifactId>
      <version>1.0.0-SNAPSHOT</version>
    </dependency>


## TODO: Your project.clj

## Kick off you REPL

Start up your REPL (in your favourite tool), you should see something like this:

    > lein do clean, repl

    Compiling sparkling.example.tfidf
    nREPL server started ...
    REPL-y 0.3.1
    Clojure 1.6.0
        Docs: (doc function-name-here)
              (find-doc "part-of-name-here")
      Source: (source function-name-here)
     Javadoc: (javadoc java-object-or-class-here)
        Exit: Control+D or (exit) or (quit)
     Results: Stored in vars *1, *2, *3, an exception in *e

    sparkling.example.tfidf=>



Require the sparkling namespaces you will need for this guide.

    (require '[sparkling.conf :as conf])
    ;;  nil

    (require '[sparkling.api :as spark])
    ;;  nil


<a name="initializing">

## Initializing Sparkling

The first step is to create a Spark configuration object, SparkConf, which contains information about your application. This is used to construct a SparkContext object which tells Spark how to access a cluster.

Here we create a SparkConf object with the string `local` to run in local mode:


    (def c (-> (conf/spark-conf)
               (conf/master "local")
               (conf/app-name "sparkling-example")))
    ;;  #'sparkling.example.tfidf/c


    (def sc (spark/spark-context c))
    ;;  #'sparkling.example.tfidf/sc



*** TODO: Move to seperate guide ***
*** TODO: Add yarn ***
*** TODO: Add link to deployment section ***
The `master` url string parameter can be one of the following formats:

|  Master URL         | Meaning                                                                                                   |
|---------------------|-----------------------------------------------------------------------------------------------------------|
| `local`             | Use one worker thread to run Spark locally (no parallelism).                                              |
| `local[N]`          | Use `N` worker threads to run Spark locally.                                                              |
| `local[*]`          | Use the same number of threads as cores to run Spark locally. <br> _Only_ available for Spark 1.0.0+      |
| `spark://HOST:PORT` | Connect to a [standalone Spark cluster](https://spark.apache.org/docs/0.9.1/spark-standalone.html) master.|
| `mesos://HOST:PORT` | Connect to a [Mesos](https://spark.apache.org/docs/0.9.1/running-on-mesos.html) cluster.                  |



Hard-coding the value of `master` and other configuration parameters can be avoided by passing the values to Spark when running `spark-submit` (Spark 1.0.0) or by allowing `spark-submit` to read these properties from a configuration file. See [Standalone Applications](#running-sparkling) for information on running sparkling applications and see Spark's [documentation](http://spark.apache.org/docs/latest/configuration.html) for more details about configuring Spark properties.

Using any of the `local` parameters provides you with a standalone system, where you do not need to install other software besides your Clojure environment (like JDK, lein, etc.) So it's great for in-REPL-development, and unit testing.

<a name="rdds"/>

## Resilient Distributed Datasets

The main abstraction Spark provides is a _resilient distributed dataset_, RDD, which is a fault-tolerant collection of elements partitioned across the nodes of the cluster that can be operated on in parallel. There are two ways to create RDDs: _parallelizing_ / _parallelizing-pairs_ an existing collection in your driver program, or referencing a dataset in an external storage system, such as a shared filesystem, HDFS, HBase, or any data source offering a Hadoop InputFormat, or even JDBC.

### RDDs and PairRDDs
RDDs basically come in two flavours. Plain RDDs simply hold a collection of arbitrary objects. PairRDDs provide a key-value-collection, but unlike a Map it can contain multiple instances of a single key. Internally, PairRDDs are constructed as collections of Scala Tuple2 objects, but Sparkling provides clojuresque ways to access these.

### Parallelized Collections

Plain RDDs in Sparkling are created by calling the `parallelize` function on your Clojure data structure:

    (def data (spark/parallelize sc ["a" "b" "c" "d" "e"]))
    ;;  #'sparkling.example.tfidf/data

Check out the contents of you newly created RDD:

    (spark/first data)
    ;;  "a"


PairRDDs in Sparkling are created by calling the `parallelize-pairs` function on your Clojure data structure:

    (def data (spark/parallelize-pairs sc [ (spark/tuple "a" 1) (spark/tuple "b" 2) (spark/tuple "c" 3) (spark/tuple "d" 4) (spark/tuple "e" 5)]))
    ;;  #'sparkling.example.tfidf/data

Once initialized, the distributed dataset or RDD can be operated on in parallel.

An important parameter for parallel collections is the number of slices to cut the dataset into. Spark runs one task for each slice of the cluster. Normally, Spark tries to set the number of slices automatically based on your cluster. However, you can also set it manually in sparkling by passing it as a third parameter to parallelize:

    (def data (spark/parallelize sc [1 2 3 4 5] 4))
    ;;  #'sparkling.example.tfidf/data

### External Datasets

Spark can create RDDs from any storage source supported by Hadoop, including the local file system, HDFS, Cassandra, HBase, Amazon S3, etc. Spark supports text files, SequenceFiles, and any other Hadoop InputFormat.

Text file RDDs can be created in sparkling using the `text-file` function under the `sparkling.api` namespace. This function takes a URI for the file (either a local path on the machine, or a `hdfs://...`, `s3n://...`, etc URI) and reads it as a collection of lines. Note, `text-file` supports S3 and HDFS globs.
The following example refers to the data.txt file at the current directory. Make sure to have one.

    (def data (spark/text-file sc "data.txt"))
    ;;  #'sparkling.example.tfidf/data

<a name="rdd-operations">
## RDD Operations

RDDs support two types of operations:

* [_transformations_](#rdd-transformations), which create a new dataset from an existing one
* [_actions_](#rdd-actions), which return a value to the driver program after running a computation on the dataset

<a name="basics">
### Basics

To illustrate RDD basics in sparkling, consider the following simple application using this sample [`data.txt`](https://github.com/gorillalabs/sparkling/blob/develop/data.txt).


    (-> (spark/text-file sc "data.txt")   ;; returns an unrealized lazy dataset
        (spark/map count)  ;; returns RDD array of length of lines
        (spark/reduce +)) ;; returns a value, should be 1406
    ;; > 1406


The first line defines a base RDD from an external file. The dataset is not loaded into memory; it is merely a pointer to the file. The second line defines an RDD of the lengths of the lines as a result of the `map` transformation. Note, the lengths are not immediately computed due to laziness. Finally, we run `reduce` on the transformed RDD, which is an action, returning only a _value_ to the driver program.

If we also wanted to reuse the resulting RDD of length of lines in later steps, we could insert:

    (spark/cache)


before the `reduce` action, which would cause the line-lengths RDD to be saved to memory after the first time it is realized. See [RDD Persistence](#rdd-persistence) for more on persisting and caching RDDs in sparkling.

<a name="sparkling-functions">
### Passing Functions to sparkling

Spark’s API relies heavily on passing functions in the driver program to run on the cluster. Flambo makes it easy and natural to define serializable Spark functions/operations and provides two ways to do this. So, in order for your functions to be available on the cluster,
the namespaces containing them need to be (AOT-)compiled. That's usually no problem, because you should uberjar your project to deploy it to the Cluster anyhow (see below *** TODO: Add Link ***).















When we evaluate this `map` transformation on the initial RDD, the result is another RDD. The result of this transformation can be seen using the `spark/collect` action to return all of the elements of the RDD. The following example will only work in an AOT-compiled environment. So, it will not work in your REPL:

    (-> (spark/parallelize sc [1 2 3 4 5])
        (spark/map (fn [x] (* x x)))
        spark/collect)

We can also use `spark/first` or `spark/take` to return just a subset of the data.

    (-> (spark/parallelize sc [1 2 3 4 5])
        (spark/map test-compiled/square)
        (spark/take 2))
    ;; [1 4]

* TODO: More work to be done from here *

<a name="key-value-pairs">

### Working with Key-Value Pairs

While most Spark operations work on RDDs containing any type of objects, a few special operations are only available on RDDs of key-value pairs. The most common ones are distributed "shuffle" operations, such as grouping or aggregating the elements by a key.

In sparkling, these operations are available on RDDs of (key, value) tuples. Flambo handles all of the transformations/serializations to/from `Tuple`, `Tuple2`, `JavaRDD`, `JavaPairRDD`, etc., so you only need to define the sequence of operations you'd like to perform on your data.

The following code uses the `reduce-by-key` operation on key-value pairs to count how many times each word occurs in a file:

```clojure
(ns yourproject.some-namespace
  (:require [sparkling.api :as spark]
            [clojure.string :as s]))

(-> (spark/text-file sc "data.txt")
    (spark/flat-map (spark/fn [l] (s/split l #" ")))
    (spark/map (spark/fn [w] [w 1]))
    (spark/reduce-by-key (spark/fn [x y] (+ x y))))
```

After the `reduce-by-key` operation, we can sort the pairs alphabetically using `spark/sort-by-key`. To collect the word counts as an array of objects in the repl or to write them to a filesysten, we can use the `spark/collect` action:

```clojure
(ns yourproject.some-namespace
  (:require [sparkling.api :as spark]
            [clojure.string :as s]))

(-> (spark/text-file sc "data.txt")
    (spark/flat-map (spark/fn [l] (s/split l #" ")))
    (spark/map (spark/fn [w] [w 1]))
    (spark/reduce-by-key (spark/fn [x y] (+ x y)))
    spark/sort-by-key
    spark/collect
    clojure.pprint/pprint)
```

<a name="rdd-transformations">
### RDD Transformations

Flambo supports the following RDD transformations:

* `map`: returns a new RDD formed by passing each element of the source through a function.
* `map-to-pair`: returns a new `JavaPairRDD` of (K, V) pairs by applying a function to all elements of an RDD.
* `reduce-by-key`: when called on an RDD of (K, V) pairs, returns an RDD of (K, V) pairs where the values for each key are aggregated using a reduce function.
* `flat-map`: similar to `map`, but each input item can be mapped to 0 or more output items (so the function should return a collection rather than a single item)
* `filter`: returns a new RDD containing only the elements of the source RDD that satisfy a predicate function.
* `join`: when called on an RDD of type (K, V) and (K, W), returns a dataset of (K, (V, W)) pairs with all pairs of elements for each key.
* `left-outer-join`: performs a left outer join of a pair of RDDs. For each element (K, V) in the first RDD, the resulting RDD will either contain all pairs (K, (V, W)) for W in second RDD, or the pair (K, (V, nil)) if no elements in the second RDD have key K.
* `sample`: returns a 'fraction' sample of an RDD, with or without replacement, using a random number generator 'seed'.
* `combine-by-key`: combines the elements for each key using a custom set of aggregation functions. Turns an RDD of (K, V) pairs into a result of type (K, C), for a 'combined type' C. Note that V and C can be different -- for example, one might group an RDD of type (Int, Int) into an RDD of type (Int, List[Int]). Users must provide three functions:
    - createCombiner, which turns a V into a C (e.g., creates a one-element list)
    - mergeValue, to merge a V into a C (e.g., adds it to the end of a list)
    - mergeCombiners, to combine two C's into a single one.
* `sort-by-key`: when called on an RDD of (K, V) pairs where K implements ordered, returns a dataset of (K, V) pairs sorted by keys in ascending or descending order, as specified by the optional boolean ascending argument.
* `coalesce`: decreases the number of partitions in an RDD to 'n'. Useful for running operations more efficiently after filtering down a large dataset.
* `group-by`: returns an RDD of items grouped by the return value of a function.
* `group-by-key`: groups the values for each key in an RDD into a single sequence.
* `flat-map-to-pair`: returns a new `JavaPairRDD` by first applying a function to all elements of the RDD, and then flattening the results.

<a name="rdd-actions">
### RDD Actions

Flambo supports the following RDD actions:

* `reduce`: aggregates the elements of an RDD using a function which takes two arguments and returns one. The function should be commutative and associative so that it can be computed correctly in parallel.
* `count-by-key`: only available on RDDs of type (K, V). Returns a map of (K, Int) pairs with the count of each key.
* `foreach`: applies a function to all elements of an RDD.
* `fold`: aggregates the elements of each partition, and then the results for all the partitions using an associative function and a neutral 'zero value'.
* `first`: returns the first element of an RDD.
* `count`: returns the number of elements in an RDD.
* `collect`: returns all the elements of an RDD as an array at the driver process.
* `distinct`: returns a new RDD that contains the distinct elements of the source RDD.
* `take`: returns an array with the first n elements of the RDD.
* `glom`: returns an RDD created by coalescing all elements of the source RDD within each partition into a list.
* `cache`: persists an RDD with the default storage level ('MEMORY_ONLY').

<a name="rdd-persistence">
## RDD Persistence

Spark provides the ability to persist (or cache) a dataset in memory across operations. Spark’s cache is fault-tolerant – if any partition of an RDD is lost, it will automatically be recomputed using the transformations that originally created it. Caching is a key tool for iterative algorithms and fast interactive use. Like Spark, sparkling provides the functions `spark/persist` and `spark/cache` to persist RDDs. `spark/persist` sets the storage level of an RDD to persist its values across operations after the first time it is computed. Storage levels are available in the `sparkling.api/STORAGE-LEVELS` map. This can only be used to assign a new storage level if the RDD does not have a storage level set already. `cache` is a convenience function for using the default storage level, 'MEMORY_ONLY'.

```clojure
(ns yourproject.some-namespace
  (:require [sparkling.api :as spark]))

(let [line-lengths (-> (spark/text-file sc "data.txt")
                       (spark/map (spark/fn [s] (count s)))
                       spark/cache)]
  (-> line-lengths
      (spark/reduce (spark/fn [x y] (+ x y)))))
```

<a name="running-sparkling">
## Standalone Applications

To run your sparkling application as a standalone application using the Spark API, you'll need to package your application in an uberjar using `lein` and execute it with:

* `SPARK_CLASSPATH`, if running Spark 0.9.1
* `./bin/spark-submit`, if running Spark 1.0.0

```shell
$ lein uberjar
...

$ SPARK_CLASSPATH=uberjar.jar spark-class com.some.class.with.main --flag1 arg1 --flag2 arg2
...
<output>

$ spark-submit --class com.some.class.with.main uberjar.jar --flag1 arg1 --flag2 arg2
...
<output>
```

<a name="kryo">
## Kryo

Flambo requires that Spark is configured to use kryo for serialization. This is configured by default using system properties.

If you need to register custom serializers, extend `sparkling.kryo.BaseFlamboRegistrator` and override its `register` method. Finally, configure your SparkContext to use your custom registrator by setting `spark.kryo.registrator` to your custom class.

There is a convenience macro for creating registrators, `sparkling.kryo.defregistrator`. The namespace where a registrator is defined should be AOT compiled.

Here is an Example (this won't work in your REPL):

```clojure
(ns yourproject.some-namespace
  (:require [sparkling.kryo :as kryo])
  (:import [flameprincess FlamePrincessHeat FlamePrincessHeatSerializer]))

(kryo/defregistrator flameprincess [kryo]
  (.register kryo FlamePrincessHeat (FlamePrincessHeatSerializer.)))

(def c (-> (conf/spark-conf)
       (conf/set "spark.kryo.registrator" flameprincess)))
```

<a name="acknowledgements">
## Acknowledgements

Thanks to The Climate Corporation and their open source project [clj-spark](https://github.com/TheClimateCorporation/clj-spark) which served as the starting point for this project.

Thanks to [Ben Black](https://github.com/b) for doing the work on the streaming api.