(ns sparkling.serialization-test
  (:require [sparkling.kryoserializer :as ks]
            [clojure.test :refer :all]
            [sparkling.conf :as conf])
  (:import [sparkling.scalaInterop ScalaFunction0]
           ))





(deftest new-registrator
  (let [conf (-> (conf/spark-conf)
                 (conf/set "spark.kryo.registrator" "sparkling.test_registrator.SerializationTestRegistrator") ;; see the underscore. That's from namespace-munging
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))
        kryo (ks/kryo-serializer conf)
        testthing (ScalaFunction0.)]

    (println (.getRegistration kryo ScalaFunction0))


    (testing
      "we can serialize and deserialize maps"
      (is (= 42
             (ks/round-trip kryo testthing))))
    ))