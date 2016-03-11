(ns sparkling.ml.transform
 (:import [org.apache.spark.ml.feature StandardScaler]))

(defn standard-scaler
  "Scales the input-col to have a range between 0 and 1"
  [{:keys [input-col output-col] :as m}]
  {:pre [(every? m [:input-col :output-col])]}
  (doto (StandardScaler.)
    (.setInputCol input-col)
    (.setOutputCol output-col)))
