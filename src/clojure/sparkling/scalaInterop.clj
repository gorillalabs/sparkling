(ns sparkling.scalaInterop
  (:import [sparkling.scalaInterop ScalaFunction0 ScalaFunction1]
           [scala Tuple1 Tuple2 Tuple3 Tuple4 Tuple5 Tuple6 Tuple7 Tuple8 Tuple9 Tuple10 Tuple11 Tuple12 Tuple13 Tuple14 Tuple15 Tuple16 Tuple17 Tuple18 Tuple19 Tuple20 Tuple21 Tuple22 Some]
           [scala.reflect ClassTag$])
  )



(defn class-tag [class]
  (.apply ClassTag$/MODULE$ class))

;; lol scala
(def ^:no-doc OBJECT-CLASS-TAG (class-tag java.lang.Object))



(defmacro gen-function
  [clazz wrapper-name]

  `(defn ~wrapper-name [f#]
     (new ~(symbol (str "sparkling.scalaInterop." clazz)) f#)))


(gen-function ScalaFunction0 function0)
(gen-function ScalaFunction1 function1)


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
   (Tuple1. a))
  ([a b]
   (Tuple2. a b))
  ([a b c]
   (Tuple3. a b c))
  ([a b c d]
   (Tuple4. a b c d))
  ([a b c d e]
   (Tuple5. a b c d e))
  ([a b c d e f]
   (Tuple6. a b c d e f))
  ([a b c d e f g]
   (Tuple7. a b c d e f g))
  ([a b c d e f g h]
   (Tuple8. a b c d e f g h))
  ([a b c d e f g h i]
   (Tuple9. a b c d e f g h i))
  ([a b c d e f g h i j]
   (Tuple10. a b c d e f g h i j))
  ([a b c d e f g h i j k]
   (Tuple11. a b c d e f g h i j k))
  ([a b c d e f g h i j k l]
   (Tuple12. a b c d e f g h i j k l))
  ([a b c d e f g h i j k l m]
   (Tuple13. a b c d e f g h i j k l m))
  ([a b c d e f g h i j k l m n]
   (Tuple14. a b c d e f g h i j k l m n))
  ([a b c d e f g h i j k l m n o]
   (Tuple15. a b c d e f g h i j k l m n o))
  ([a b c d e f g h i j k l m n o p]
   (Tuple16. a b c d e f g h i j k l m n o p))
  ([a b c d e f g h i j k l m n o p q]
   (Tuple17. a b c d e f g h i j k l m n o p q))
  ([a b c d e f g h i j k l m n o p q r]
   (Tuple18. a b c d e f g h i j k l m n o p q r))
  ([a b c d e f g h i j k l m n o p q r s]
   (Tuple19. a b c d e f g h i j k l m n o p q r s))
  ([a b c d e f g h i j k l m n o p q r s t]
   (Tuple20. a b c d e f g h i j k l m n o p q r s t))
  ([a b c d e f g h i j k l m n o p q r s t & [u v :as args]]
   (case (count args)
     1 (Tuple21. a b c d e f g h i j k l m n o p q r s t u)
     2 (Tuple22. a b c d e f g h i j k l m n o p q r s t u v)
     (throw (ex-info "Can only create Scala tuples with up to 22 elements"
                     {:count (+ 20 (count args))})))))


(defn tuple-reader [v]
  (apply tuple v))


(defmethod print-method Tuple2 [^Tuple2 o ^java.io.Writer w]
  (.write w (str "#sparkling/tuple " (pr-str [(._1 o) (._2 o)]))))

(defmethod print-dup Tuple2 [o w]
  (print-method o w))

(defn some-or-nil [option]
  (when (instance? Some option)
    (.get ^Some option)))



#_(defn Function0-apply [this]
  (-apply this))

#_(defn Function1-apply [this x]
  (-apply this x))

