---
title: "Getting Started with Sparkling"
layout: article
description: "Tutorial for writing Spark jobs in Clojure: cloning a getting-started-repo, starting the REPL, working with datasets."
---

# Getting started

## About this guide

This guide combines an overview of Sparkling with a quick tutorial that helps you to get started with it.
It should take about 20 minutes to read and study the provided code examples. This guide covers:

 * [Cloning a base repository for starting to explore Sparkling](#start)
 * Experience Sparkling / Spark by
   * [get going in the REPL](#REPL)
   * [starting up a `local` SparkContext](#initializing)
   * [parallelizing data to a Spark Dataset](#rdds)
   * [reading data from a file into a Spark Dataset](#external)
   * [performing transformations on the Datasets](#rdd-operations)
   * [working with key-value-pairs](#keyvalue)
   * [calling actions on the Datasets.](#rdd-actions)



## <a name="start"/>Starting point
There is a [companion project](https://github.com/gorillalabs/sparkling-getting-started) to this getting started guide. Just use this as a staring point to explore Sparkling, because it contains a ready-made project.clj for [Leiningen](http://leiningen.org/). There's no need to install Apache Spark, or even to run a cluster of any kind. Just get going on your notebook.

Clone that repo by executing

    git clone https://github.com/gorillalabs/sparkling-getting-started.git
    cd sparkling-getting-started


in your shell.


## <a name="REPL"/>Kick off you REPL

Start up your REPL (in your favourite tool), you should see something like this (after downloading required dependencies):

    $ lein do clean, repl

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

{% highlight clojure %}
(require '[sparkling.conf :as conf])
;;  nil

(require '[sparkling.core :as spark])
;;  nil
{% endhighlight %}




## <a name="initializing"/>Initializing Sparkling

The first step is to create a Spark configuration object, SparkConf, which contains information about your application. This is used to construct a SparkContext object which tells Spark how to access a cluster.

Here we create a SparkConf object with the string `local` to run in local mode:


{% highlight clojure %}
(def c (-> (conf/spark-conf)
           (conf/master "local")
           (conf/app-name "sparkling-example")))
;;  #'sparkling.example.tfidf/c


(def sc (spark/spark-context c))
;;  #'sparkling.example.tfidf/sc
{% endhighlight %}


Using a `local` master provides you with a standalone system, where you do not need to install other software besides your Clojure environment (like JDK, lein, etc.) So it's great for in-REPL-development, and unit testing.



## <a name="rdds"/>Resilient Distributed Datasets

The main abstraction Spark provides is a _resilient distributed dataset_, RDD, which is a fault-tolerant collection of elements partitioned across the nodes of the cluster that can be operated on in parallel. There are two ways to create RDDs: _parallelizing_ / _parallelizing-pairs_ an existing collection in your driver program, or referencing a dataset in an external storage system, such as a shared filesystem, HDFS, HBase, or any data source offering a Hadoop InputFormat, or even JDBC.

### RDDs and PairRDDs
RDDs basically come in two flavors. Plain RDDs simply hold a collection of arbitrary objects. PairRDDs provide a key-value-collection, but unlike a Map it can contain multiple instances of a single key. Internally, PairRDDs are constructed as collections of Scala Tuple2 objects, but Sparkling provides clojuresque ways to access these.

### Parallelized Collections

Plain RDDs in Sparkling are created by calling the `parallelize` function on your Clojure data structure:

{% highlight clojure %}
(def data (spark/parallelize sc ["a" "b" "c" "d" "e"]))
;;  #'sparkling.example.tfidf/data
{% endhighlight %}

Check out the contents of you newly created RDD:

{% highlight clojure %}
(spark/first data)
;;  "a"
{% endhighlight %}


PairRDDs in Sparkling are created by calling the `parallelize-pairs` function on your Clojure data structure:

{% highlight clojure %}
(def data (spark/parallelize-pairs sc [ (spark/tuple "a" 1) (spark/tuple "b" 2) (spark/tuple "c" 3) (spark/tuple "d" 4) (spark/tuple "e" 5)]))
;;  #'sparkling.example.tfidf/data
{% endhighlight %}

Once initialized, the distributed dataset or RDD can be operated on in parallel.

An important parameter for parallel collections is the number of slices to cut the dataset into. Spark runs one task for each slice of the cluster. Normally, Spark tries to set the number of slices automatically based on your cluster. However, you can also set it manually in sparkling by passing it as a third parameter to parallelize:

{% highlight clojure %}
(def data (spark/parallelize sc [1 2 3 4 5] 4))
;;  #'sparkling.example.tfidf/data
{% endhighlight %}

### <a name="external"/>External Datasets

Spark can create RDDs from any storage source supported by Hadoop, including the local file system, HDFS, Cassandra, HBase, Amazon S3, etc. Spark supports text files, SequenceFiles, and any other Hadoop InputFormat.

Text file RDDs can be created in sparkling using the `text-file` function under the `sparkling.core` namespace. This function takes a URI for the file (either a local path on the machine, or a `hdfs://...`, `s3n://...`, etc URI) and reads it as a collection of lines. Note, `text-file` supports S3 and HDFS globs.
The following example refers to the data.txt file at the current directory. Make sure to have one.

{% highlight clojure %}
(def data (spark/text-file sc "data.txt"))
;;  #'sparkling.example.tfidf/data
{% endhighlight %}


## <a name="rdd-operations"/>RDD Operations

RDDs support two types of operations:

* [_transformations_](#rdd-transformations), which create a new dataset from an existing one
* [_actions_](#rdd-actions), which return a value to the driver program after running a computation on the dataset



### <a name="basics"/>Basics

To illustrate RDD basics in sparkling, consider the following simple application using this sample [`data.txt`](https://github.com/gorillalabs/sparkling/blob/develop/data.txt).


{% highlight clojure %}
(->> (spark/text-file sc "data.txt")   ;; returns an unrealized lazy dataset
     (spark/map count)  ;; returns RDD array of length of lines
     (spark/reduce +)) ;; returns a value, should be 1406
;; > 1406
{% endhighlight %}


The first line defines a base RDD from an external file. The dataset is not loaded into memory; it is merely a pointer to the file. The second line defines an RDD of the lengths of the lines as a result of the `map` transformation. Note, the lengths are not immediately computed due to laziness. Finally, we run `reduce` on the transformed RDD, which is an action, returning only a _value_ to the driver program.

If we also wanted to reuse the resulting RDD of length of lines in later steps, we could insert:

{% highlight clojure %}
(spark/cache)
{% endhighlight %}


before the `reduce` action, which would cause the line-lengths RDD to be saved to memory after the first time it is realized. See [RDD Persistence](#rdd-persistence) for more on persisting and caching RDDs in sparkling.


### <a name="sparkling-functions"/>Passing Functions to sparkling

Spark’s API relies heavily on passing functions in the driver program to run on the cluster. Sparkling makes it easy and natural to define serializable Spark functions/operations and provides two ways to do this. So, in order for your functions to be available on the cluster,
the namespaces containing them need to be (AOT-)compiled. That's usually no problem, because you should uberjar your project to deploy it to the Cluster anyhow.


When we evaluate this `map` transformation on the initial RDD, the result is another RDD. The result of this transformation can be seen using the `spark/collect` action to return all of the elements of the RDD. The following example will only work in an AOT-compiled environment. So, it will not work in your REPL:

{% highlight clojure %}
(->> (spark/parallelize sc [1 2 3 4 5])
     (spark/map (fn [x] (* x x)))
     spark/collect)
{% endhighlight %}

We can also use `spark/first` or `spark/take` to return just a subset of the data.




### <a name="keyvalue"/> Working with Key-Value Pairs

Some transformation in Spark operate on Key-Value-Tuples, e.g. joins, reduce-by-key, etc. In sparkling, these operations are available on PairRDDs.
You do not need to deal with the internal data structures of Apache Spark (like scala.Tuple2), if you use the functions from the `sparkling.destructuring` namespace.

So, first require that namespace

{% highlight clojure %}
(require '[sparkling.destructuring :as s-de])
{% endhighlight %}

We deal with strings, so require clojure.string also:

{% highlight clojure %}
(require '[clojure.string :as s])
{% endhighlight %}


The following code uses the `reduce-by-key` operation on key-value pairs to count how many times each word occurs in a file:


{% highlight clojure %}
(->> (spark/text-file sc "data.txt")
     (spark/flat-map (fn [l] (s/split l #" ")))
     (spark/map-to-pair (fn [w] (spark/tuple w 1)))
     (spark/reduce-by-key +)
     (spark/map (s-de/key-value-fn (fn [k v] (str k " appears " v " times."))))
     )
;; #<JavaPairRDD org.apache.spark.api.java.JavaPairRDD@4c3c63f1>

(spark/take  3 *1)
;; ["created appears 1 times." "under appears 1 times." "this appears 4 times."]
{% endhighlight %}

After the `reduce-by-key` operation, we can sort the pairs alphabetically using `spark/sort-by-key`. To collect the word counts as an array of objects in the repl or to write them to a filesysten, we can use the `spark/collect` action:

{% highlight clojure %}
(->> (spark/text-file sc "data.txt")
     (spark/flat-map (fn [l] (s/split l #" ")))
     (spark/map-to-pair (fn [w] (spark/tuple w 1)))
     (spark/reduce-by-key +)
     spark/sort-by-key
     (spark/map (s-de/key-value-fn (fn [k v] [k v])))
     spark/collect
     clojure.pprint/pprint)
;; [["" 4] ["But" 1] ["Four" 1] ["God" 1] ["It" 3] ["Liberty" 1] ["Now" 1] ["The" 2] ["We" 2] ["a" 7] ...
;; nil
{% endhighlight %}

### <a name="destructuring-sugar"/> Destructuring sugar

This section describes a syntactic convenience. It can safely be skipped.

In the example above, `spark/sort-by-key` produces a PairRDD, whose elements are `scala/Tuple2` instances. We then need to unwrap these to get the raw values inside.
This is what `s-de/key-value-fn` does; it is one of several such wrappers provided.
In addition to destructuring tuples, there are two other "wrapper" classes which can be produced by RDD operations:

* `scala.collection.convert.Wrappers$IterableWrapper` for collections, for example as in the result of `group-by-key`
* `com.google.common.base.Optional` for potentially absent values, for example as in the result of `left-outer-join`

For this reason, the `sparkling.destructuring/fn` macro implements a destructuring binding form specialized for these data types.

In the last example above, `(s-de/key-value-fn (fn [k v] [k v]))` could be replaced by `(s-de/fn (k v) [k v])`, which is not much different. However `s-de/fn` supports arbitrary nesting of tuples, a binding symbol beginning with a `-` will be unwrapped as a seq, and a binding symbol beginning with a `?` will be unwrapped as an `Optional`, as illustrated in the following example:

{% highlight clojure %}

(def votes (s/parallelize-pairs sc [(s/tuple "Woody Allen" :upvote)
                                    (s/tuple "Woody Allen" :upvote)
                                    (s/tuple "Genghis Khan" :downvote)
                                    (s/tuple "Genghis Khan" :downvote)
                                    (s/tuple "Bugs Bunny" :upvote)]))
(def votes-by-user (s/group-by-key votes))
(def reputation-by-user (s/parallelize-pairs sc [(s/tuple "Woody Allen"  {:reputation 1})
                                                 (s/tuple "Genghis Khan" {:reputation 3})]))
(->> (s/left-outer-join votes-by-user reputation-by-user)
     (s/first))
;; #sparkling/tuple ["Woody Allen" #sparkling/tuple [#object[scala.collection.convert.Wrappers$IterableWrapper 0x12461ef5 "[:upvote, :upvote]"] #object[com.google.common.base.Present 0x28e67e03 "Optional.of({:reputation 1})"]]]

{% endhighlight %}

Notice that the `left-outer-join` results in a PairRDD where each member is a Tuple2 containing a name and a nested Tuple2 containing a collection of events, and an optional state. The `s-de/fn` can make the code to process this RDD cleaner:

{% highlight clojure %}

(->> (s/left-outer-join votes-by-user reputation-by-user)
     (s/map-values
      (s-de/fn (-events ?rep)            ;; <-- the specialized tuple-destructuring binding form
        (reduce (fn [state event]
                  (update-in state [:reputation] (condp = event :upvote inc :downvote dec)))
                (or rep {:reputation 0}) ;; Bugs Bunny is not represented in `reputation-by-users`, so `rep` will be nil
                events)))                ;; events is a clojure seq, not a scala collection wrapper
     (s/collect))
;; [#sparkling/tuple ["Woody Allen" {:reputation 3}] #sparkling/tuple ["Bugs Bunny" {:reputation 1}] #sparkling/tuple ["Genghis Khan" {:reputation 1}]]
    
{% endhighlight %}

The binding form above `(-events ?rep)` means: "Expect a Tuple2, containing a collection and an optional".
The symbols `events` and `rep` will be bound inside the fn, already "unwrapped". Arbitrarily nested tuples are supported. Other familiar destructuring forms will not work in a `s-de/fn` signature, it is specifically for `Tuple*`, `Optional`, and `IterableWrapper`.

### <a name="rdd-transformations"/>RDD Transformations

Sparkling supports the following RDD transformations:

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



### <a name="rdd-actions"/>RDD Actions

Sparkling supports the following RDD actions:

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



## <a name="rdd-persistence"/>RDD Persistence

Spark provides the ability to persist (or cache) a dataset in memory across operations. Spark’s cache is fault-tolerant – if any partition of an RDD is lost, it will automatically be recomputed using the transformations that originally created it. Caching is a key tool for iterative algorithms and fast interactive use. Like Spark, sparkling provides the functions `spark/persist` and `spark/cache` to persist RDDs. `spark/persist` sets the storage level of an RDD to persist its values across operations after the first time it is computed. Storage levels are available in the `sparkling.core/STORAGE-LEVELS` map. This can only be used to assign a new storage level if the RDD does not have a storage level set already. `cache` is a convenience function for using the default storage level, 'MEMORY_ONLY'.


    (let [line-lengths (->> (spark/text-file sc "data.txt")
                            (spark/map count)
                            spark/cache)]
      (->> line-lengths
          (spark/reduce +)))
    ;; 1406


## <a name="reading"/>Further reading
We will provide you with further guides, e.g. on deploying your project to a Spark Cluster in the future. So, please stay tuned and check [our guides section](/sparkling/articles/guides.html) from time to time.


## <a name="next"/>Next steps?

Star / watch the Sparkling Github Repo to keep up to date.

<iframe src="https://ghbtns.com/github-btn.html?user=gorillalabs&repo=sparkling&type=star&count=true&size=large" frameborder="0" scrolling="0" width="160px" height="30px"></iframe>
<iframe src="https://ghbtns.com/github-btn.html?user=gorillalabs&repo=sparkling&type=watch&count=true&size=large&v=2" frameborder="0" scrolling="0" width="160px" height="30px"></iframe>
