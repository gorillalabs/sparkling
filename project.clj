(defproject gorillalabs/sparkling "1.2.4.1-SNAPSHOT"
            :description "A Clojure Library for Apache Spark"
            :url "https://gorillalabs.github.io/sparkling/"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :scm {:name "git"
                  :url  "https://github.com/gorillalabs/sparkling"}
            :dependencies [[org.clojure/tools.logging "0.3.1"]
                           [org.clojure/clojure "1.7.0" :scope "provided"]
                           [clj-time "0.9.0"]
                           [com.twitter/carbonite "1.4.0" :exclusions [com.twitter/chill-java]]

                           [com.damballa/parkour "0.6.2" :exclusions [com.thoughtworks.paranamer/paranamer]]
                           [com.damballa/abracad "0.4.12" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                           [org.apache.avro/avro-mapred "1.7.7" :scope "provided" :classifier "hadoop2" :exclusions [org.slf4j/slf4j-api io.netty/netty commons-lang org.mortbay.jetty/servlet-api]]

                           ;[com.esotericsoftware.kryo/kryo "2.24.0" :scope "provided"]

                           ]

            :aliases {"all" ["with-profile" "default"]
                      }
  :profiles {:default      [:base :system :user :provided :spark-1.6.0 :dev :debug]
             :dev          {:dependencies   [[criterium "0.4.3"]]
                            :plugins        [[lein-dotenv "RELEASE"]
                                             [jonase/eastwood "0.1.4"]
                                             [lein-kibit "0.1.2"]
                                             [lein-marginalia "0.8.0" :exclusions [org.clojure/clojure com.google.guava/guava]]
                                             [lein-ancient "0.5.4" :exclusions [org.clojure/clojure org.clojure/tools.reader commons-codec]]
                                             [lein-release "1.0.5" :exclusions [org.clojure/clojure]]
                                             [gargamel "0.5.0"]
                                             [lein-pprint "1.1.1"]]
                            :resource-paths ["data"]
                            ;; so gen-class stuff works in the repl
                            :aot            [sparkling.api
                                             sparkling.function
                                             sparkling.scalaInterop
                                             ]}
             :jenkins      {:plugins [[lein-test-out "0.3.1"]]
                            }

             :spark-1.1.0  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.1.0"]
                                                     ]}
             :spark-1.4.0  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.4.0"]
                                                     ]}

             :spark-1.5.0  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.5.0"]
                                                     ]}

             :spark-1.6.0  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.6.0"]
                                                     [com.github.fommil.netlib/all "1.1.2" :extension "pom"]
                                                     [com.googlecode.matrix-toolkits-java/mtj "1.0.2"]
                                                     [org.apache.spark/spark-mllib_2.10 "1.6.0" ]
                                                     ] }

             :hadoop-2.6.0 ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.hadoop/hadoop-client "2.6.0"
                                                      :exclusions [commons-codec org.apache.curator/curator-recipes org.slf4j/slf4j-api com.google.guava/guava io.netty/netty org.apache.curator/curator-framework org.apache.zookeeper/zookeeper]]
                                                     [org.apache.hadoop/hadoop-hdfs "2.6.0" :exclusions [com.google.guava/guava io.netty/netty]]]}

             :spark-1.2.1  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.2.1" :scope "provided"
                                                      :exclusions [commons-net commons-codec commons-io]
                                                      ]
                                                     [commons-net "3.1"]
                                                     [commons-codec "1.4"]
                                                     [commons-io "2.4"]]}
             :spark-1.3.1  ^{:pom-scope :provided} {:dependencies
                                                    [[org.apache.spark/spark-core_2.10 "1.3.1"]]}

             :debug {:debug true
                     :injections [(prn (into {} (System/getProperties)))]}
             :test         {:resource-paths ["dev-resources" "data"]
                            :jvm-opts ^:replace ["-server" "-Xmx2g"]
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
                                             sparkling.ml.classification
                                             sparkling.ml.core
                                             sparkling.ml.transform
                                             sparkling.ml.validation]}
             }
            :source-paths ["src/clojure"]
            :java-source-paths ["src/java"]
            :codox {:defaults                  {:doc/format :markdown}
                    :include                   [sparkling.core sparkling.conf sparkling.kryo sparkling.broadcast sparkling.debug sparkling.destructuring]
                    :output-dir                "doc"
                    :src-dir-uri               "https://raw.githubusercontent.com/gorillalabs/sparkling/v1.2.2/"
                    :src-linenum-anchor-prefix "L"}
            :javac-options ["-Xlint:unchecked" "-source" "1.6" "-target" "1.7"]
            :jvm-opts ^:replace ["-server" "-Xmx1g"]
            :global-vars {*warn-on-reflection* false}
            :lein-release {:deploy-via :clojars}
            )

;; test with
;;     lein do clean, with-profile +spark-1.1.0 test
