(ns sparkling.utils
  (:require [clojure.tools.logging :as log]))

(defn trace [msg]
  (fn [x]
    (log/trace msg x)
    x))

(defn truthy? [x]
  (if x (Boolean/TRUE) (Boolean/FALSE)))

