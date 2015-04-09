;; ## Utilities and macros for dealing with kryo
;;
(ns sparkling.kryo
  (:import [scala.reflect ClassTag$]))

;; lol scala
(def ^:no-doc OBJECT-CLASS-TAG (.apply ClassTag$/MODULE$ java.lang.Object))


(defmacro defregistrator
  "DEPRECATED. DO NOT USE THIS ANYMORE. Use sparkling.serialization instead.
  A macro for creating a custom kryo registrator for application that need to
  register custom kryo serializers.

  This macro must be called from a namespace that is AOT compiled. This is not typically
  an issue since application jars are packaged as uberjars.

  Note that we are extending `BaseRegistrator` and not spark's `KryoRegistrator`."
  {:deprecated "1.2"}
  [name & register-impl]
  (let [prefix (gensym)
        classname (str *ns* ".registrator." name)]
    `(do
       (gen-class :name ~classname
                  :extends sparkling.serialization.BaseRegistrator
                  :prefix ~prefix)
       (defn ~(symbol (str prefix "register"))
         ~@register-impl)
       (def ~name ~classname))))
