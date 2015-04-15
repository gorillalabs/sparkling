(ns sparkling.rdd.hadoopAvro-test
  (:require [sparkling.rdd.hadoopAvro :refer :all]
            [sparkling.testutils.records.registrator]
            [sparkling.api :as s]
            [sparkling.conf :as conf]
            [abracad.avro :as avro]
            [clojure.java.io]
            [clojure.test :refer :all]
            [sparkling.testutils.records.domain :as domain]))

(defn test-schema "Loads a schema" []
  (with-open [schema-stream (clojure.java.io/input-stream "data/avro/twitter.avsc")]
    (avro/parse-schema schema-stream)))

;; Thanks to Michael G. Noll (https://github.com/miguno)
;; I copied his avro test files from https://github.com/miguno/avro-hadoop-starter

(deftest hadoopAvro

  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator" "sparkling.testutils.records.registrator.Registrator")
                 (conf/master "local[*]")
                 (conf/app-name "hadoop-avro-test"))]
    (s/with-context c conf
                    (testing
                        "load stuff from hadoop using AVRO"
                      (is (= (s/collect (load-avro-file c "data/avro/twitter.avro" :requires ['sparkling.testutils.records.domain])) ;; the requires is not necessary here, as it will be required from the task-deserializer. However, in your cluster you might need it.
                             [(domain/map->tweet {:username "miguno", :tweet "Rock: Nerf paper, scissors is fine.", :timestamp 1366150681})
                              (domain/map->tweet {:username "BlizzardCS", :tweet "Works as intended.  Terran is IMBA.", :timestamp 1366154481})
                              (domain/map->tweet {:username "DarkTemplar", :tweet "From the shadows I come!", :timestamp 1366154681})
                              (domain/map->tweet {:username "VoidRay", :tweet "Prismatic core online!", :timestamp 1366160000})
                              (domain/map->tweet {:username "VoidRay", :tweet "Fire at will, commander.", :timestamp 1366160010})
                              (domain/map->tweet {:username "DarkTemplar", :tweet "I am the blade of Shakuras!", :timestamp 1366174681})
                              (domain/map->tweet {:username "Immortal", :tweet "I return to serve!", :timestamp 1366175681})
                              (domain/map->tweet {:username "Immortal", :tweet "En Taro Adun!", :timestamp 1366176283})
                              (domain/map->tweet {:username "VoidRay", :tweet "There is no greater void than the one between your ears.", :timestamp 1366176300})
                              (domain/map->tweet {:username "DarkTemplar", :tweet "I strike from the shadows!", :timestamp 1366184681})]
                             )))

                    #_(testing
                      "load stuff from hadoop using AVRO"
                      (is (=
                            (do
                            (save-avro-file c (s/parallelize c [
                                                                  {:username "miguno", :tweet "Rock: Nerf paper, scissors is fine.", :timestamp 1366150681}
                                                                  {:username "BlizzardCS", :tweet "Works as intended.  Terran is IMBA.", :timestamp 1366154481}
                                                                  ]) (test-schema) "data/avro/test-out.avro")
                            (s/collect (load-avro-file c "data/avro/test-out.avro")))
                            [
                             {:username "miguno", :tweet "Rock: Nerf paper, scissors is fine.", :timestamp 1366150681}
                             {:username "BlizzardCS", :tweet "Works as intended.  Terran is IMBA.", :timestamp 1366154481}
                             ])))
                    )))