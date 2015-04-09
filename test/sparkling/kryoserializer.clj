(ns sparkling.kryoserializer
  (:require [carbonite.buffer :as buffer]
            [sparkling.serialization])
  (:import [org.apache.spark.serializer KryoSerializer]
           )
  )

(defn kryo-serializer [conf]
  (let [kryo (.newKryo (KryoSerializer. conf))]
    kryo))

(defn round-trip [registry o]
  (->> o
       (buffer/write-bytes registry)
       (buffer/read-bytes registry)))