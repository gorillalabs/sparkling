(defproject gorillalabs/sparkling "1.2.1-SNAPSHOT"
            :description "A Clojure Library for Apache Spark"
            :url "https://github.com/chrisbetz/sparkling"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :scm {:name "git"
                  :url  "https://github.com/gorillalabs/sparkling"}
            :dependencies [[org.clojure/clojure "1.6.0" :scope "provided"]
                           [org.clojure/tools.logging "0.3.1"]
                           [org.slf4j/slf4j-api "1.7.5" :scope "provided"]
                           [clj-time "0.8.0"]
                           [com.twitter/carbonite "1.4.0"
                            :exclusions [com.twitter/chill-java com.esotericsoftware.kryo/kryo]]
                           [com.twitter/chill_2.10 "0.5.2"
                            :exclusions [org.scala-lang/scala-library com.esotericsoftware.kryo/kryo]]
                           [com.damballa/parkour "0.6.2"
                            ; :exclusions [org.apache.avro/avro org.codehaus.jackson/jackson-mapper-asl org.codehaus.jackson/jackson-core-asl]
                            ]
                           [com.damballa/abracad "0.4.11"
                            ; :exclusions [org.apache.avro/avro com.fasterxml.jackson.core/jackson-core com.fasterxml.jackson.dataformat/jackson-dataformat-smile]
                            ]
                           ;[org.xerial.snappy/snappy-java "1.1.1.6" :scope "provided"]
                           ;[org.scala-lang/scala-library "2.10.4" :scope "provided"]
                           [com.esotericsoftware.kryo/kryo "2.24.0" :scope "provided"]
                           ;[commons-codec "1.10" :scope "provided"]


                           [org.apache.spark/spark-core_2.10 "1.2.1" :scope "provided"
                            :exclusions [commons-net commons-codec commons-io]]
                           [commons-net "3.1"]
                           [commons-codec "1.4"]
                           [commons-io "2.4"]


                           [org.apache.avro/avro "1.7.7" :scope "provided"
                            :exclusions [com.thoughtworks.paranamer/paranamer]]
                           [org.apache.avro/avro-mapred "1.7.7" :scope "provided" :classifier "hadoop2"
                            :exclusions [org.mortbay.jetty/jetty-util
                                         org.mortbay.jetty/jetty
                                         org.mortbay.jetty/servlet-api]]]

            :aliases {"all" ["with-profile" "dev:dev,spark-1.1.0,hadoop-2.6.0:dev,spark-1.2.1,hadoop-2.6.0"]
                      }
            :profiles {:dev                {:dependencies   [[criterium "0.4.3"]]
                                            :plugins        [[lein-dotenv "RELEASE"]
                                                             [lein-marginalia "0.8.0" :exclusions [org.clojure/clojure com.google.guava/guava]]
                                                             [lein-ancient "0.5.4" :exclusions [org.clojure/clojure org.clojure/tools.reader commons-codec]]
                                                             [codox "0.8.10" :exclusions [org.clojure/clojure]]
                                                             [lein-release "1.0.5" :exclusions [org.clojure/clojure]]
                                                             [lein-pprint "1.1.1"]]
                                            :resource-paths ["data"]
                                            ;; so gen-class stuff works in the repl
                                            :aot            [sparkling.api
                                                             sparkling.function
                                                             sparkling.scalaInterop]}
                       :jenkins            {:plugins [[lein-test-out "0.3.1"]]
                                            }

                       :spark-1.1.0        {:dependencies
                                            [[org.apache.spark/spark-core_2.10 "1.1.0"]
                                             ]}

                       :hadoop-2.6.0       ^{:pom-scope :provided} {:dependencies
                                                                    [[org.apache.hadoop/hadoop-client "2.6.0"
                                                                      :exclusions [commons-codec org.apache.curator/curator-recipes org.slf4j/slf4j-api com.google.guava/guava io.netty/netty org.apache.curator/curator-framework org.apache.zookeeper/zookeeper]]
                                                                     [org.apache.hadoop/hadoop-hdfs "2.6.0" :exclusions [com.google.guava/guava io.netty/netty]]]}

                       :spark-1.2.1        ^{:pom-scope :provided} {:dependencies
                                                                    [[org.apache.spark/spark-core_2.10 "1.2.1" :scope "provided"
                                                                      :exclusions [commons-net commons-codec commons-io]
                                                                      ]
                                                                     [commons-net "3.1"]
                                                                     [commons-codec "1.4"]
                                                                     [commons-io "2.4"]]}

                       :test               {:resource-paths ["dev-resources" "data"]
                                            :aot            [sparkling.core
                                                             sparkling.api
                                                             sparkling.function
                                                             sparkling.scalaInterop
                                                             sparkling.destructuring
                                                             sparkling.debug
                                                             sparkling.rdd.hadoopAvro
                                                             sparkling.rdd.jdbc
                                                             sparkling.testutils.records.domain
                                                             sparkling.testutils.records.registrator
                                                             sparkling.api-test
                                                             sparkling.core-test
                                                             sparkling.function-test
                                                             sparkling.conf-test
                                                             sparkling.rdd.hadoopAvro-test
                                                             sparkling.rdd.jdbc-test
                                                             sparkling.accumulator-test
                                                             sparkling.test-registrator
                                                             sparkling.serialization-test
                                                             ]}
                       }
            :source-paths ["src/clojure"]
            :java-source-paths ["src/java"]
            :codox {:defaults                  {:doc/format :markdown}
                    :include                   [sparkling.core sparkling.conf sparkling.kryo sparkling.broadcast sparkling.debug sparkling.destructuring]
                    :output-dir                "doc"
                    :src-dir-uri               "https://raw.githubusercontent.com/gorillalabs/sparkling/1.1.0/"
                    :src-linenum-anchor-prefix "L"}
            :javac-options ["-Xlint:unchecked" "-source" "1.6" "-target" "1.6"]
            :jvm-opts ^:replace ["-server" "-Xmx1g"]
            :global-vars {*warn-on-reflection* false}
            :lein-release {:deploy-via :clojars}
            )

;; test with
;;     lein do clean, with-profile +spark-1.1.0 test
