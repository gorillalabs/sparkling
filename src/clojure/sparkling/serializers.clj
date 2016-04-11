(ns sparkling.serializers
    (:require [clj-time.core :a t]
              )
    (:import [clojure.lang PersistentTreeSet APersistentVector$SubVector]
             [org.joda.time DateTime DateTimeZone]
             [com.esotericsoftware.kryo Serializer Kryo]
             [com.esotericsoftware.kryo.io Output Input]))

(def PersistentTreeMap-serializer
  (proxy [Serializer] []
         (write [^Kryo registry, ^Output output, ^PersistentTreeSet coll]
           (.writeInt output (count coll) true)
           (try
             #_(let [b (sfn/serialize (.comparator coll))]
               (.writeInt output (count b) true)
               (.writeBytes output b))
             (.writeClassAndObject registry output (.comparator coll))
             (catch Exception e (throw (RuntimeException. (str "Error serializing PersistentTreeMap comparator "
                                                               (.comparator coll) " with meta "
                                                               (meta (.comparator coll))) e)))
             )
           (doseq [x coll]
                  (.writeClassAndObject registry output x))
           )
         (read [^Kryo registry, ^Input input, ^Class s-type]
           (let [len (.readInt input true)
                 ;b-len (.readInt input true)
                 ;bytes (.readBytes input b-len)
                 ;_ (trace "Read " (String. bytes (java.nio.charset.Charset/forName "UTF-8")))
                 ; comp (try (sfn/deserialize bytes)
                 ;          (catch Exception e (throw (RuntimeException. (str "Error deserializing PersistentTreeMap comparator with byte-len " b-len " and content " bytes) e))))
                 comp (.readClassAndObject registry input)
                 ]
                (->> (repeatedly len #(.readClassAndObject registry input))
                     (apply sorted-set-by comp)))
           )))


(defn sorted-collections []
      [[PersistentTreeSet PersistentTreeMap-serializer]])


(def SubVector-serializer
  (proxy [Serializer] []
         (write [^Kryo registry, ^Output output, ^APersistentVector$SubVector coll]
           (.writeInt output (count coll) true)
           (doseq [x coll]
                  (.writeClassAndObject registry output x)))
         (read [^Kryo registry, ^Input input, ^Class s-type]
           (let [len (.readInt input true)]
             (into [] (repeatedly len #(.readClassAndObject registry input)))))))


(defn ordered-collections []
  [[APersistentVector$SubVector SubVector-serializer]])


(def DateTime-serializer
  (proxy [Serializer] []
         (write [^Kryo registry, ^Output output, ^DateTime datetime]
           (.writeLong output (.getMillis datetime), true)
           (.writeString output (.getID (.getZone datetime))))
         (read [^Kryo registry, ^Input input, ^Class s-type]
           (let [millis (.readLong input true),
                 timezone (DateTimeZone/forID (.readString input))]
             (DateTime. millis timezone)))))


(defn joda-serializers []
      [[DateTime DateTime-serializer]])
