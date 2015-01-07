# Sparkling - A Clojure API for Apache Spark

Sparkling is a Clojure API for [Apache Spark](http://spark.apache.org/).

Check out our site for information about [Gorillalabs Sparkling](http://gorillalabs.github.io/sparkling/) and a [getting started guide](http://gorillalabs.github.io/sparkling/articles/getting_started.html).


[![Build Status](https://secure.travis-ci.org/gorillalabs/sparkling.png)](http://travis-ci.org/gorillalabs/sparkling)

# Why another Spark API?
See Release Notes for Version 1.0.0

# Release Notes

## 1.0.0 - Added value to the existing libraries (clj-spark and flambo)

* It's about twice as fast by getting rid of a reflection call (thanks to [David Jacot](https://github.com/dajac) for his take on this).
* Get rid of mapping/remapping inside the api functions, which 
   * bloated the execution plan (mine shrinked to a third) and
   * (more importantly) allowed me to keep partitioner information.
* adding more -values functions (e.g. map-values), againt to keep partitioner information.
* Additional Sources for RDDs:
  * JdbcRDD: Reading Data from your JDBC source.
  * Hadoop-Avro-Reader: Reading AVRO Files from HDFS


Sparkling is available from Clojars. To use with Leiningen, add

[![Clojars Project](http://clojars.org/gorillalabs/sparkling/latest-version.svg)](http://clojars.org/gorillalabs/sparkling) to your dependencies.


See [gorillalabs/sparkling-getting-started](https://github.com/gorillalabs/sparkling-getting-started) for an example project using Sparkling.
This one is also used in the [getting started guide](http://gorillalabs.github.io/sparkling/articles/getting_started.html)


# Acknowledgements

Thanks to The Climate Corporation and their open source [clj-spark](https://github.com/TheClimateCorporation/clj-spark) project, and to
Yieldbot for [yieldbot/flambo](https://github.com/yieldbot/flambo) which served as the starting point for this project.

# License

Copyright (C) 2014-2015 Dr. Christian Betz, and the Gorillalabs team.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
