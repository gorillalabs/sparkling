# Sparkling - A Clojure API for Apache Spark

Sparkling is a Clojure API for [Apache Spark](http://spark.apache.org/).

Check out our site for information about [Gorillalabs Sparkling](http://gorillalabs.github.io/sparkling/) and a [getting started guide](http://gorillalabs.github.io/sparkling/articles/getting_started.html).

[![Build Status](https://secure.travis-ci.org/gorillalabs/sparkling.png)](http://travis-ci.org/gorillalabs/sparkling)

# Availabilty from Clojars
Sparkling is available from Clojars. To use with Leiningen, add

[![Clojars Project](http://clojars.org/gorillalabs/sparkling/latest-version.svg)](http://clojars.org/gorillalabs/sparkling) to your dependencies.


See [gorillalabs/sparkling-getting-started](https://github.com/gorillalabs/sparkling-getting-started) for an example project using Sparkling.
This one is also used in the [getting started guide](http://gorillalabs.github.io/sparkling/articles/getting_started.html)

# Release Notes

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


# Acknowledgements

Thanks to The Climate Corporation and their open source clj-spark project, and to
Yieldbot for yieldbot/flambo which served as the starting point for this project.

# License

Copyright (C) 2014-2015 Dr. Christian Betz, and the Gorillalabs team.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
