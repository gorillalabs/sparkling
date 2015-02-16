(defproject gorillalabs/sparkling "1.2.0-SNAPSHOT"
            :description "A Clojure Library for Apache Spark"
            :url "https://github.com/chrisbetz/sparkling"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :scm {:name "git"
                  :url  "https://github.com/gorillalabs/sparkling"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/tools.logging "0.3.1"]
                           [com.twitter/carbonite "1.4.0"
                            :exclusions [com.twitter/chill-java]]
                           [com.twitter/chill_2.10 "0.5.2"
                            :exclusions [org.scala-lang/scala-library]]
                           ]

            :aliases {"all" ["with-profile" "dev,spark-1.1.0,hadoop-2.6.0,avro-1.7.7-hadoop2:dev,spark-1.2.1,hadoop-2.6.0,avro-1.7.7-hadoop2"]
                      }
            :profiles {:dev                {:dependencies   [[midje "1.6.3" :exclusions [joda-time commons-codec]]
                                                             [commons-codec "1.10"]
                                                             [criterium "0.4.3"]]
                                            :plugins        [[lein-marginalia "0.8.0"]
                                                             [lein-ancient "0.5.4"]
                                                             [codox "0.8.9"]
                                                             [lein-release "1.0.5"]
                                                             [lein-pprint "1.1.1"]]
                                            :resource-paths ["data"]
                                            ;; so gen-class stuff works in the repl
                                            :aot            [sparkling.api
                                                             sparkling.function
                                                             sparkling.scalaInterop]}
                       :jenkins            {:plugins [[lein-test-out "0.3.1"]]
                                            }
                       :default            [:base :system :user :spark-1.2.1 :hadoop-2.6.0 :avro-1.7.7-hadoop2 :provided :dev]

                       :avro-1.7.7-hadoop2 ^:leaky {:dependencies [[org.apache.avro/avro "1.7.7" :exclusions [org.codehaus.jackson/jackson-mapper-asl org.codehaus.jackson/jackson-core-asl]]
                                                           [org.apache.avro/avro-mapred "1.7.7" :classifier "hadoop2" :exclusions [io.netty/netty commons-lang com.thoughtworks.paranamer/paranamer org.slf4j/slf4j-log4j12 org.mortbay.jetty/servlet-api org.codehaus.jackson/jackson-mapper-asl org.codehaus.jackson/jackson-core-asl]]
                                                           [com.damballa/parkour "0.6.1" :exclusions [org.codehaus.jackson/jackson-mapper-asl org.codehaus.jackson/jackson-core-asl]]
                                                           [com.damballa/abracad "0.4.11" :exclusions [org.apache.avro/avro]]]}
                       :spark-1.1.0        ^:leaky {:dependencies
                                            [[org.apache.spark/spark-core_2.10 "1.1.0" :exclusions [commons-io com.thoughtworks.paranamer/paranamer]]
                                             #_[org.apache.spark/spark-streaming_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                             #_[org.apache.spark/spark-streaming-kafka_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                             #_[org.apache.spark/spark-sql_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer org.scala-lang/scala-compiler com.fasterxml.jackson.core/jackson-databind]]
                                             ]}

                       :hadoop-2.6.0       ^:leaky {:dependencies
                                            [[org.apache.hadoop/hadoop-client "2.6.0" :exclusions [org.apache.curator/curator-recipes io.netty/netty org.apache.curator/curator-framework com.google.guava/guava commons-codec commons-net org.slf4j/slf4j-api org.apache.zookeeper/zookeeper]]
                                             [org.apache.hadoop/hadoop-hdfs "2.6.0" :exclusions [io.netty/netty com.google.guava/guava]]]}

                       :spark-1.2.1        ^:leaky {:dependencies
                                            [[org.apache.spark/spark-core_2.10 "1.2.1" :exclusions [commons-net commons-io com.thoughtworks.paranamer/paranamer]]
                                             #_[org.apache.spark/spark-streaming_2.10 "1.2.1" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                             #_[org.apache.spark/spark-streaming-kafka_2.10 "1.2.1" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                             #_[org.apache.spark/spark-sql_2.10 "1.2.1" :exclusions [com.thoughtworks.paranamer/paranamer org.scala-lang/scala-compiler com.fasterxml.jackson.core/jackson-databind]]
                                             ]}

                       :test               {:resource-paths ["dev-resources" "data"]
                                            :aot            [sparkling.core
                                                             sparkling.api
                                                             sparkling.function
                                                             sparkling.scalaInterop
                                                             sparkling.destructuring
                                                             sparkling.debug
                                                             sparkling.rdd.hadoopAvro
                                                             sparkling.rdd.jdbc
                                                             sparkling.api-test
                                                             sparkling.function-test
                                                             sparkling.conf-test
                                                             sparkling.rdd.hadoopAvro-test
                                                             sparkling.rdd.jdbc-test
                                                             ]}
                       }
            :source-paths ["src/clojure"]
            :java-source-paths ["src/java"]
            :codox {:defaults                  {:doc/format :markdown}
                    :include                   [sparkling.api sparkling.conf sparkling.kryo sparkling.broadcast sparkling.debug sparkling.destructuring]
                    :output-dir                "doc"
                    :src-dir-uri               "https://raw.githubusercontent.com/gorillalabs/sparkling/1.0.0/"
                    :src-linenum-anchor-prefix "L"}
            :javac-options ["-Xlint:unchecked" "-source" "1.6" "-target" "1.6"]
            :jvm-opts ^:replace ["-server" "-Xmx1g"]
            :global-vars {*warn-on-reflection* false}
            :lein-release {:deploy-via :clojars}
            )

;; test with
;;     lein do clean, with-profile +spark-1.1.0 test


