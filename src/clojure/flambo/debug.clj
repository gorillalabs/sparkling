(ns flambo.debug
  (:require [flambo.api :as f]
            [clojure.tools.logging :refer :all])
  )

(defn inspect [rdd name]
  (let [cached (-> rdd
                   (f/cache )
                   (f/rdd-name  name))]
    (try
      (info name "/Partitioner: " (f/partitioner cached) ", #partitions:" (f/count-partitions cached))
      (catch Throwable t))
    (let [c (f/count cached)]
      (info "#items@" name ": " c)
      (when-not (zero? c)
        (info "first items@" name ": " (f/first cached))))
    cached))