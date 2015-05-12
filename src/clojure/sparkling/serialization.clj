(ns sparkling.serialization
  (:require [clojure.tools.logging :as log]
            [carbonite.api :as carbonite]
            [sparkling.core :as core]
            [sparkling.serializers :as serializer])
  (:import [com.twitter.chill Tuple2Serializer Tuple3Serializer]
           [org.objenesis.strategy StdInstantiatorStrategy]
           [org.apache.spark.serializer KryoRegistrator]
           [scala Tuple2 Tuple3 None$]
           [com.esotericsoftware.kryo Kryo Serializer KryoSerializable]
           [scala.collection.mutable WrappedArray$ofRef ArrayBuffer]
           [java.util ArrayList Currency EnumSet List]
           [clojure.lang RT$DefaultComparator MethodImplCache]
           [org.apache.spark.util.collection CompactBuffer]
           [scala.collection.immutable Nil$]))

(def register-class (memfn #^Kryo register #^Class clazz))
(def register-class-with-id (memfn #^Kryo register #^Class clazz id))
(def register-class-with-serializer (memfn #^Kryo register #^Class clazz #^Serializer serializer))
(def register-class-with-serializer-and-id (memfn #^Kryo register #^Class clazz #^Serializer serializer id))

(defn register [#^Kryo kryo #^Class clazz & {:keys [#^Serializer serializer id]}]
  (cond (and serializer id)
        (register-class-with-serializer-and-id kryo clazz serializer id)
        serializer
        (register-class-with-serializer kryo clazz serializer)
        id
        (register-class-with-id kryo clazz id)
        :default
        (register-class kryo clazz)))

(defn register-array-type [^Kryo kryo ^Class type & {:keys [#^Serializer serializer id]}]
  (let [fully-qualified-name (.getName type)]
    (register kryo
              (Class/forName (str "[L" fully-qualified-name ";"))
              :serializer serializer
              :id id
              )))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Register actuall classes


(defn register-native-array-serializers [^Kryo kryo]

  (register kryo (Class/forName "[B"))                      ;; byte array
  (register kryo (Class/forName "[C"))                      ;; char array
  (register kryo (Class/forName "[S"))                      ;; short array
  (register kryo (Class/forName "[I"))                      ;; int array
  (register kryo (Class/forName "[J"))                      ;; long array
  (register kryo (Class/forName "[F"))                      ;; float array
  (register kryo (Class/forName "[D"))                      ;; double array
  (register kryo (Class/forName "[Z"))                      ;; boolean array
  )

(defn register-optional [kryo classname]
  (when-let [clazz (try (Class/forName classname)
                        (catch Exception _
                          ;ignore, because not finding this class is exactly what I'm up to!
                          nil))]
    (register kryo clazz)))

(defn register-java-class-serializers [^Kryo kryo]
  (register kryo BigInteger)
  (register kryo ArrayList)
  (register kryo Class)
  (register kryo Object)
  (register-array-type kryo Object)
  (register-array-type kryo String)
  (register-array-type kryo List)
  (register-array-type kryo BigInteger)
  (register-array-type kryo BigDecimal)
  (register-array-type kryo Class)
  (register-array-type kryo Enum)
  (register-array-type kryo EnumSet)
  (register-array-type kryo Currency)
  (register-array-type kryo StringBuffer)
  (register-array-type kryo StringBuilder)
  )

(defn register-clojure [^Kryo kryo]
  #_(register kryo AFunction)
  #_(register kryo clojure.lang.AFunction$1)
  (register kryo MethodImplCache)
  (register kryo RT$DefaultComparator)
  )

(defn register-scala [^Kryo kryo]
  (register kryo Tuple2 :serializer (Tuple2Serializer.))
  (register-array-type kryo Tuple2)

  (register kryo Tuple3 :serializer (Tuple3Serializer.))
  (register-array-type kryo Tuple3)

  (register kryo WrappedArray$ofRef)
  (register kryo ArrayBuffer)
  (register-array-type kryo ArrayBuffer)
  (register kryo scala.reflect.ClassTag$$anon$1)
  (register kryo scala.reflect.ManifestFactory$$anon$2)
  (register kryo None$)
  (register kryo Nil$)
  (register kryo scala.reflect.ManifestFactory$$anon$10)
  )

(defn register-spark [^Kryo kryo]
  (register-array-type kryo CompactBuffer)
  (register kryo org.apache.spark.util.collection.OpenHashMap$mcJ$sp)
  (register kryo org.apache.spark.util.collection.OpenHashSet)
  (register kryo org.apache.spark.util.collection.OpenHashSet$Hasher)
  (register kryo org.apache.spark.util.collection.BitSet)
  (register kryo org.apache.spark.util.collection.OpenHashMap$$anonfun$1)
  (register kryo org.apache.spark.util.collection.OpenHashMap$$anonfun$2))







(defn register-base-classes [kryo]
  (require 'clojure.tools.logging)
  (require 'carbonite.api)
  (require 'sparkling.core)
  (require 'sparkling.serializers)
  (require 'sparkling.destructuring)

  ;(log/info "Registering base classes for kryo")
  (carbonite/default-registry kryo)
  (carbonite/register-serializers kryo (serializer/sorted-collections))
  (carbonite/register-serializers kryo (serializer/joda-serializers))

  (register-native-array-serializers kryo)
  (register-java-class-serializers kryo)
  (register-clojure kryo)
  (register-scala kryo)
  (register-spark kryo)

  (register-array-type kryo KryoSerializable)
  (register kryo sparkling.function.Function))

(deftype Registrator []
  KryoRegistrator
  (#^void registerClasses [#^KryoRegistrator this #^Kryo kryo]
    (try
      (.setInstantiatorStrategy kryo (StdInstantiatorStrategy.))
      (require 'sparkling.serialization)
      (require 'clojure.tools.logging)
      (register-base-classes kryo)

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



