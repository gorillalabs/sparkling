(ns sparkling.kryoserializer
  (:import [com.esotericsoftware.kryo Kryo]
           [sparkling.kryo BaseFlamboRegistrator]
           [org.apache.spark.serializer KryoRegistrator])
  (:require [carbonite.buffer :as buffer]))

(defn kryo-serializer [& {:keys [^KryoRegistrator registrator] :or {registrator (BaseFlamboRegistrator.)}}]
  (let [kryo (Kryo.)]
    (.registerClasses registrator kryo)
    kryo))

(defn round-trip [registry o]
  (->> o
       (buffer/write-bytes registry)
       (buffer/read-bytes registry)))