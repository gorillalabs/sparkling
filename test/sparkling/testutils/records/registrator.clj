(ns sparkling.testutils.records.registrator
  (:require [sparkling.serialization :as standard-registrator]
            [sparkling.testutils.records.domain]
            )
  (:import [org.apache.spark.serializer KryoRegistrator]
           [com.esotericsoftware.kryo Kryo Serializer]
           [org.objenesis.strategy StdInstantiatorStrategy]
           [com.esotericsoftware.kryo.io Output Input]))

(def tweet-serializer
  (proxy
    [Serializer] []
    (write [#^Kryo registry, #^Output output, tweet]
      (.writeString output (.username tweet))
      (.writeString output (.tweet tweet))
      (.writeLong output (.timestamp tweet))
      )
    (read [#^Kryo registry, #^Input input, #^Class _]
      (let [username (.readString input)
            tweet (.readString input)
            timestamp (.readLong input)
            ]
        (sparkling.testutils.records.domain/->tweet username tweet timestamp)))))

(deftype Registrator []
  KryoRegistrator
  (#^void registerClasses [#^KryoRegistrator this #^Kryo kryo]
    (try
      (.setInstantiatorStrategy kryo (StdInstantiatorStrategy.))
      ; (.setRegistrationRequired kryo true)
      (standard-registrator/register-base-classes kryo)
      (standard-registrator/register kryo sparkling.testutils.records.domain.tweet :serializer tweet-serializer)
      (standard-registrator/register-array-type kryo sparkling.testutils.records.domain.tweet)

      (catch Exception e
        (RuntimeException. "Failed to register kryo!" e)))))