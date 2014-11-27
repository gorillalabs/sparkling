(ns flambo.scalaInterop
  (:require [serializable.fn :as sfn]
            [flambo.utils :as u]
            [flambo.kryo :as kryo]
            [clojure.tools.logging :as log])
  (:import [scala Function0 Function1 Tuple2]
           [java.io Serializable]))

(defn- serfn? [f]
  (= (type f) :serializable.fn/serializable-fn))

(def serialize-fn sfn/serialize)
(def deserialize-fn (memoize sfn/deserialize))
(def array-of-bytes-type (Class/forName "[B"))

;; ## Generic
(defn -init
  "Save the function f in state"
  [f]
  [[] f])

(defn -apply [this & xs]
  (let [fn-or-serfn (.state this)
        f (if (instance? array-of-bytes-type fn-or-serfn)
            (binding [sfn/*deserialize* kryo/deserialize]
              (deserialize-fn fn-or-serfn))
            fn-or-serfn)]
    #_(log/trace "CLASS" (type this))
    #_(log/trace "META" (meta f))
    #_(log/trace "XS" xs)
    (apply f xs)))

;; ## Functions
(defn mk-sym
  [fmt sym-name]
  (symbol (format fmt sym-name)))

(defmacro gen-function
  [clazz wrapper-name]
  (let [new-class-sym (mk-sym "flambo.scalaInterop.%s" clazz)
        prefix-sym (mk-sym "%s-" clazz)]
    `(do
       (def ~(mk-sym "%s-init" clazz) -init)
       (def ~(mk-sym "%s-apply" clazz) -apply)
       (gen-class
         :name ~new-class-sym
         :implements [~(mk-sym "scala.%s" clazz) Serializable]
         :prefix ~prefix-sym
         :init ~'init
         :state ~'state
         :constructors {[Object] []})
       (defn ~wrapper-name [f#]
         (new ~new-class-sym
              (if (serfn? f#) (binding [sfn/*serialize* kryo/serialize]
                                (serialize-fn f#)) f#))))))


(gen-function Function0 function0)
(gen-function Function1 function1)


(defn tuple
  "Returns a Scala tuple. Uses the Scala tuple class that matches the
  number of arguments.

  ($/tuple 1 2) => (instance-of scala.Tuple2)
  (apply $/tuple (range 20)) => (instance-of scala.Tuple20)
  (apply $/tuple (range 21)) => (instance-of scala.Tuple21)
  (apply $/tuple (range 22)) => (instance-of scala.Tuple22)
  (apply $/tuple (range 23)) => (throws ExceptionInfo)"
  {:added "0.1.0"}
  ([a]
   (scala.Tuple1. a))
  ([a b]
   (scala.Tuple2. a b))
  ([a b c]
   (scala.Tuple3. a b c))
  ([a b c d]
   (scala.Tuple4. a b c d))
  ([a b c d e]
   (scala.Tuple5. a b c d e))
  ([a b c d e f]
   (scala.Tuple6. a b c d e f))
  ([a b c d e f g]
   (scala.Tuple7. a b c d e f g))
  ([a b c d e f g h]
   (scala.Tuple8. a b c d e f g h))
  ([a b c d e f g h i]
   (scala.Tuple9. a b c d e f g h i))
  ([a b c d e f g h i j]
   (scala.Tuple10. a b c d e f g h i j))
  ([a b c d e f g h i j k]
   (scala.Tuple11. a b c d e f g h i j k))
  ([a b c d e f g h i j k l]
   (scala.Tuple12. a b c d e f g h i j k l))
  ([a b c d e f g h i j k l m]
   (scala.Tuple13. a b c d e f g h i j k l m))
  ([a b c d e f g h i j k l m n]
   (scala.Tuple14. a b c d e f g h i j k l m n))
  ([a b c d e f g h i j k l m n o]
   (scala.Tuple15. a b c d e f g h i j k l m n o))
  ([a b c d e f g h i j k l m n o p]
   (scala.Tuple16. a b c d e f g h i j k l m n o p))
  ([a b c d e f g h i j k l m n o p q]
   (scala.Tuple17. a b c d e f g h i j k l m n o p q))
  ([a b c d e f g h i j k l m n o p q r]
   (scala.Tuple18. a b c d e f g h i j k l m n o p q r))
  ([a b c d e f g h i j k l m n o p q r s]
   (scala.Tuple19. a b c d e f g h i j k l m n o p q r s))
  ([a b c d e f g h i j k l m n o p q r s t]
   (scala.Tuple20. a b c d e f g h i j k l m n o p q r s t))
  ([a b c d e f g h i j k l m n o p q r s t & [u v :as args]]
   (case (count args)
     1 (scala.Tuple21. a b c d e f g h i j k l m n o p q r s t u)
     2 (scala.Tuple22. a b c d e f g h i j k l m n o p q r s t u v)
     (throw (ex-info "Can only create Scala tuples with up to 22 elements"
                     {:count (+ 20 (count args))})))))


(defn tuple-reader [v]
  (apply tuple v))


(defmethod print-method Tuple2 [^Tuple2 o ^java.io.Writer w]
  (.write w (str "#flambo/tuple " (pr-str [(._1 o) (._2 o)]))))

(defmethod print-dup Tuple2 [o w]
  (print-method o w))




#_(defn Function0-apply [this]
  (-apply this))

#_(defn Function1-apply [this x]
  (-apply this x))

