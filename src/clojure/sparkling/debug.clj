(ns sparkling.debug
  (:require [sparkling.api :as s]
            [clojure.tools.logging :refer :all])
  )

(defn inspect [rdd name]
  (let [cached (-> rdd
                   (s/cache )
                   (s/rdd-name  name))]
    (try
      (info name "/Partitioner: " (s/partitioner cached) ", #partitions:" (s/count-partitions cached))
      (catch Throwable t))
    (let [c (s/count cached)]
      (info "#items@" name ": " c)
      (when-not (zero? c)
        (info "first items@" name ": " (s/first cached))))
    cached))