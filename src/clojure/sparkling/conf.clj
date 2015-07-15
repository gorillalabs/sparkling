;; Functions for creating and modifying `SparkConf` objects
;;
;; The functions are designed to be used with the threading macro `->` for building
;; objects to pass into `spark-context`.
;;
;; (-> (spark-conf)
;;      (app-name "aname")
;;      (master "local[2]"))
;;
(ns sparkling.conf
  (:import [org.apache.spark SparkConf])
  (:refer-clojure :exclude (set get contains remove)))


(defn master
  ([conf]
   (master conf "local[*]"))
  ([conf master]
   (.setMaster conf master)))

(defn app-name
  [conf name]
  (.setAppName conf name))

(defn jars
  [conf jars]
  (.setJars conf (into-array String jars)))

(defn set
  ([conf key val]
   (.set conf key val))
  ([conf amap]
   (loop [c conf
          aseq (seq amap)]
     (if aseq
       (let [[k v] (first aseq)
             c (set c k v)]
         (recur c (next aseq)))
       c))))

(defn set-if-missing
  [conf key val]
  (.setIfMissing conf key val))

(defn set-executor-env
  ([conf key val]
   (.setExecutorEnv conf key val))
  ([conf amap]
   (loop [c conf
          aseq (seq amap)]
     (if aseq
       (let [[k v] (first aseq)
             c (set-executor-env c k v)]
         (recur c (next aseq)))
       c))))

(defn remove
  [conf key]
  (.remove conf key))

(defn get
  ([conf key]
   (.get conf key))
  ([conf key default]
   (.get conf key default)))

(defn get-all
  [conf]
  (into {} (for [t (.getAll conf)]
             {(._1 t) (._2 t)})))

(defn spark-home
  [conf home]
  (.setSparkHome conf home))

(defn to-string
  [conf]
  (.toDebugString conf))

(defn set-sparkling-registrator
  "Sets the kryo registrator to the one provided by Sparkling.
  Make sure to require sparkling.serialization namespace in your code, as otherwise the sparkling.serialization.Registrator class will not be available under special circumstances (REPL, dependency checkout in leiningen)...

  If you need to add your own registrations either create your own registrator or extend the Sparkling registrator with something like
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
        (RuntimeException. \"Failed to register kryo!\" e)))))
  "
  [conf]
  (set conf "spark.kryo.registrator" "sparkling.serialization.Registrator"))

(defn spark-conf
  []
  (-> (SparkConf.)
      (set "spark.serializer" "org.apache.spark.serializer.KryoSerializer")
      (set-sparkling-registrator)
      ))
