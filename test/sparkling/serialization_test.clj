(ns sparkling.serialization-test
  (:require [sparkling.serialization :as ser]
            [sparkling.kryoserializer :as ks]
            [clojure.test :refer :all]
            [sparkling.conf :as conf])
  (:import [org.apache.spark.serializer KryoRegistrator]
           [com.esotericsoftware.kryo Kryo Serializer]
           [com.esotericsoftware.kryo.io Input Output]
           [sparkling.scalaInterop ScalaFunction0]
           [sparkling.serialization Registrator]))


(extend-type Registrator
  ser/RegistrationProtocol
  (register-classes [#^KryoRegistrator _ #^Kryo kryo]
    (ser/register-class-with-serializer kryo ScalaFunction0 (proxy
                                     [Serializer] []
                                     (write [#^Kryo registry, #^Output output, thing]
                                       (println "Writing " thing)
                                       )
                                     (read [#^Kryo registry, #^Input input, #^Class clazz]
                                       (println "Reading clazz")
                                       42)))))


(deftest new-registrator
  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
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