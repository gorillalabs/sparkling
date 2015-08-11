(ns sparkling.utils
  (:require [clojure.tools.logging :as log]
            [clojure.string :as s])
  (:import (org.apache.spark.rdd RDD)))

(defn trace [msg]
  (fn [x]
    (log/trace msg x)
    x))

(defn truthy? [x]
  (if x (Boolean/TRUE) (Boolean/FALSE)))


(defn- unmangle
  "Given the name of a class that implements a Clojure function,
  returns the function's name in Clojure.
  Note: If the true Clojure function name contains any underscores
  (a rare occurrence), the unmangled name will contain hyphens
  at those locations instead."
  [classname]
  (.replace
    (s/replace
      classname
      #"^(.+)\$(.+)(|__\d+)$" "$1/$2")
    \_ \-))



(defn- sparkling-fn
  "Returns true if a stack-element represents a sparkling function or one from clojure.lang."
  [stack-element]
  (re-find
    #"^(sparkling|clojure\.lang)\."
    (.getClassName stack-element)))


(defn unmangle-fn
  "Given a function,
  returns the function's name in Clojure.
  Note: If the true Clojure function name contains any underscores
  (a rare occurrence), the unmangled name will contain hyphens
  at those locations instead."
  [f]
  (unmangle (-> f .getClass .getName)))

(defn set-auto-name [rdd & args]
  (try
    (let [stackElement (first (remove sparkling-fn (.getStackTrace (Exception.))))
          filename (.getFileName stackElement)
          classname (.getClassName stackElement)
          line-number (str "L." (.getLineNumber stackElement))
          rdd-name (str "#<"
                        (.getSimpleName (.getClass rdd))
                        ": "
                        (s/join
                          ", "
                          [(unmangle classname)
                           filename
                           line-number
                           ])
                        (if-not (empty? args)
                          (str " [" (s/join ", " args) "]")
                          "")
                        ">")]
      (.setName rdd rdd-name))
    (catch Exception e
      ;; ignore
      rdd
      )))