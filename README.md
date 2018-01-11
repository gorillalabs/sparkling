# Sparkling - A Clojure API for Apache Spark

Sparkling is a Clojure API for [Apache Spark](http://spark.apache.org/).


# Show me a small sample

```clojure
(do
  (require '[sparkling.conf :as conf])
  (require '[sparkling.core :as spark])
  (spark/with-context sc (-> (conf/spark-conf)              ; this creates a spark context from the given context
                             (conf/app-name "sparkling-test")
                             (conf/master "local"))
                      (let [lines-rdd (spark/into-rdd sc ["This is the first line"   ;; here we provide data from a clojure collection.
                                                          "Testing spark"           ;; You could also read from a text file, or avro file.
                                                          "and sparkling"           ;; You could even approach a JDBC datasource
                                                          "Happy hacking!"])]
                        (spark/collect                      ;; get every element from the filtered RDD
                          (spark/filter                     ;; filter elements in the given RDD (lines-rdd)
                            #(.contains % "spark")          ;; a pure clojure function as filter predicate
                            lines-rdd)))))
```


#  Where to find more info
Check out our site for information about [Gorillalabs Sparkling](http://gorillalabs.github.io/sparkling/)
and a [getting started guide](http://gorillalabs.github.io/sparkling/articles/getting_started.html).

# Sample Project repo available
Just clone our [getting-started repo](https://github.com/gorillalabs/sparkling-getting-started) and get going right now.

But note: There's one thing you need to be aware of: Certain namespaces need to be AOT-compiled, e.g. because the classes are referenced in the startup process by name. I'm doing this in my project.clj using the ```:aot```directive like this

```clojure
            :aot [#".*" sparkling.serialization sparkling.destructuring]
```

# Availabilty from Clojars
Sparkling is available from Clojars. To use with Leiningen, add

[![Clojars Project](http://clojars.org/gorillalabs/sparkling/latest-version.svg)](http://clojars.org/gorillalabs/sparkling) to your dependencies.

[![Build Status](https://secure.travis-ci.org/gorillalabs/sparkling.png)](http://travis-ci.org/gorillalabs/sparkling)


# Release Notes

### 2.0.0 - switch to Spark 2.0
 * added support for Spark SQL

### 1.2.3 - more developer friendly
 * added @/deref support for broadcasts Making it easier to work with broadcasts by using Clojure mechanisms. This is especially true for unit tests, as you could test without actual broadcasts, but with anything deref-able.
 * added RDD autonaming from fn metadata, eases navigation in SparkUI
 * added lookup functionality. Make sure the key to your Tuples is Serializable (Java serialization), as it will be serialized as part of your task definition, not only as part of your data. These are handled differently in Spark.
 

### 1.2.2 - added ```whole-text-files``` in sparkling.core.
 (thanks to [Jase Bell](https://github.com/jasebell))

### 1.2.1 - improved Kryo Registration, AVRO reader + new Accumulator feature
 * feature: added accumulators (Thanks to [Oleg Smirnov](https://github.com/master) for that)
 * change: overhaul of Kryo Registration: Deprecated defregistrator macro, added Registrator type (see sparkling.serialization), with basic support of required types. This introduced a breaking change (sorry!): You need to aot-compile (or require) sparkling.serialization to run stuff in the REPL.
 * feature: added support for your own avro readers, making it possible to read types/records instead of maps. Major improvement on memory consumption.

### 1.1.1 - cleaned dependencies
 * No more spilling of unwanted stuff in your application. You only need to refer to sparkling to get a proper environment with Spark 1.2.1.
   In order to deploy to a cluster with Spark pre-installed, you need to set Spark dependency to provided in your project, though.

### 1.1.0 - Added a more clojuresque API
 * Use sparkling.core instead of sparkling.api for parameter orders similar to Clojure. Easier currying using partial.
 * Made it possible to use Keywords as Functions by serializing IFn instead of AFunction.
 * Tested with Spark 1.1.0 and Spark 1.2.1.

### 1.0.0 - Added value to the existing libraries (clj-spark and flambo)

* It's about twice as fast by getting rid of a reflection call (thanks to [David Jacot](https://github.com/dajac) for his take on this).
* Get rid of mapping/remapping inside the api functions, which
   * bloated the execution plan (mine shrinked to a third) and
   * (more importantly) allowed me to keep partitioner information.
* adding more -values functions (e.g. map-values), againt to keep partitioner information.
* Additional Sources for RDDs:
  * JdbcRDD: Reading Data from your JDBC source.
  * Hadoop-Avro-Reader: Reading AVRO Files from HDFS


# Contributing

Feel free to fork the Sparkling repository, improve stuff and open up a pull request against our "develop" branch. However, we'll only add features with tests, so make sure everything is green ;)

# Acknowledgements

Thanks to The Climate Corporation and their open source clj-spark project, and to
Yieldbot for yieldbot/flambo which served as the starting point for this project.

# License

Copyright (C) 2014-2015 Dr. Christian Betz, and the Gorillalabs team.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
