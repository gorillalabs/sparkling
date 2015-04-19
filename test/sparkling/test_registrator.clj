(ns sparkling.test-registrator
  (:require [clojure.tools.logging :as log]
            [sparkling.serialization :as ser])
  (:import [com.esotericsoftware.kryo.io Input Output]
           [com.esotericsoftware.kryo Kryo Serializer]
           [sparkling.scalaInterop ScalaFunction0]
           [org.objenesis.strategy StdInstantiatorStrategy]
           [org.apache.spark.serializer KryoRegistrator]))

(deftype SerializationTestRegistrator []
  KryoRegistrator
  (#^void registerClasses [#^KryoRegistrator this #^Kryo kryo]
    (try
      (.setInstantiatorStrategy kryo (StdInstantiatorStrategy.))
      (ser/register-base-classes kryo)
      (ser/register-class-with-serializer kryo ScalaFunction0 (proxy
                                                                [Serializer] []
                                                                (write [#^Kryo registry, #^Output output, thing]
                                                                  (println "Writing " thing)
                                                                  )
                                                                (read [#^Kryo registry, #^Input input, #^Class clazz]
                                                                  (println "Reading clazz")
                                                                  42)))
      (catch Exception e
        (try
          (log/warn (str "Error registering serializer" this) e)
          (catch Exception e1
            ;(binding [*out* *err*]
            (println "Failed to not log properly the expetion " e " while registering classes from " this " to " kryo ".\nLogging failure was " e1 ".")
            ;)
            (.printStackTrace e)
            ))
        (throw (RuntimeException. "Failed to register kryo!" e))))))