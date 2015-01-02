(defproject chrisbetz/flambo "0.6.4"
            :description "A Clojure Library for Apache Spark"
            :url "https://github.com/chrisbetz/flambo"
            :license {:name "Eclipse Public License"
                      :url  "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [org.clojure/tools.logging "0.3.1"]
                           [com.twitter/carbonite "1.4.0"
                            :exclusions [com.twitter/chill-java]]
                           [com.twitter/chill_2.10 "0.5.0"
                            :exclusions [org.scala-lang/scala-library]]

                           ;; [AVRO Feature] This adds support for reading avro files
                           [com.damballa/parkour "0.6.0"]
                           [com.damballa/abracad "0.4.11" :exclusions [org.apache.avro/avro]]
                           ;; [/AVRO Feature]
                           ]

            :profiles {:dev      {:dependencies   [[midje "1.6.3"]
                                                   [criterium "0.4.3"]]
                                  :plugins        [[lein-marginalia "0.8.0"]
                                                   [lein-ancient "0.5.4"]
                                                   [codox "0.8.9"]
                                                   [lein-release "1.0.5"]
                                                   [lein-pprint "1.1.1"]]
                                  :resource-paths ["data"]
                                  ;; so gen-class stuff works in the repl
                                  :aot            [flambo.api
                                                   flambo.function
                                                   flambo.scalaInterop]}
                       :jenkins  {:plugins      [[lein-test-out "0.3.1"]]
                                  }
                       :provided {:dependencies
                                   [[org.apache.spark/spark-core_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer]]
                                    [org.apache.spark/spark-streaming_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                    [org.apache.spark/spark-streaming-kafka_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer com.fasterxml.jackson.core/jackson-databind]]
                                    [org.apache.spark/spark-sql_2.10 "1.1.0" :exclusions [com.thoughtworks.paranamer/paranamer org.scala-lang/scala-compiler com.fasterxml.jackson.core/jackson-databind]]

                                    ;; [AVRO Feature] This adds support for reading avro files
                                    [org.apache.avro/avro "1.7.6"]
                                    [org.apache.avro/avro-mapred "1.7.6" :exclusions [org.slf4j/slf4j-log4j12 org.mortbay.jetty/servlet-api com.thoughtworks.paranamer/paranamer io.netty/netty commons-lang]]
                                    ;; [/AVRO Feature]

                                    ]}
                       :test     {:resource-paths ["dev-resources" "data"]
                                  :aot            [flambo.api
                                                   flambo.function
                                                   flambo.scalaInterop
                                                   flambo.destructuring
                                                   flambo.debug
                                                   flambo.rdd.hadoopAvro
                                                   flambo.rdd.jdbc
                                                   flambo.api-test
                                                   flambo.conf-test
                                                   flambo.rdd.hadoopAvro-test
                                                   flambo.rdd.jdbc-test
                                                   ]}
                       :uberjar  {:aot :all}
                       :example  {:main         flambo.example.tfidf
                                  :source-paths ["example"]
                                  :aot          [flambo.example.tfidf]}
                       }
            :source-paths ["src/clojure"]
            :java-source-paths ["src/java"]
            :codox {:defaults                  {:doc/format :markdown}
                    :include                   [flambo.api flambo.conf flambo.kryo]
                    :output-dir                "doc/codox"
                    :src-dir-uri               "https://raw.githubusercontent.com/chrisbetz/flambo/develop/"
                    :src-linenum-anchor-prefix "L"}
            :javac-options ["-Xlint:unchecked" "-source" "1.6" "-target" "1.6"]
            :jvm-opts ^:replace ["-server" "-Xmx1g"]
            :global-vars {*warn-on-reflection* false}
            :lein-release {:deploy-via :clojars}
            )

;; run example with
;;     lein with-profile example,provided run

