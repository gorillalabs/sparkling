(ns sparkling.format
  (:require [sparkling.core :as spark])
  (:import  [org.apache.spark.sql SparkSession SparkSession$Builder]
            [org.apache.spark.sql RowFactory]
            [org.apache.spark.sql.types DataTypes]))


(def schema-types {:binary    DataTypes/BinaryType
                   :boolean   DataTypes/BooleanType
                   :byte      DataTypes/ByteType
                   :date      DataTypes/DateType
                   :double    DataTypes/DoubleType
                   :float     DataTypes/FloatType
                   :integer   DataTypes/IntegerType
                   :long      DataTypes/LongType
                   :null      DataTypes/NullType
                   :short     DataTypes/ShortType
                   :string    DataTypes/StringType
                   :timestamp DataTypes/TimestampType})


(defn row->map
  "Converts Spark Row to Clojure map using specified schema"
  [schema]
  (let [fields        (into [] (.fieldNames schema))
        offsets       (range 0 (count fields))
        field-offsets (map vector fields offsets)]
    (fn [row]
      (->> (map #(vector (keyword (first %)) (.get row (second %))) field-offsets)
           (into {})))))


(defn row->vector
  "Converst Spark Row to Clojure vector using specified schema"
  [schema]
  (let [fields  (into [] (.fieldNames schema))
        offsets (range 0 (count fields))]
    (fn [row]
      (->> (map #(.get row %) offsets)
           (into [])))))


(defn select-values
  "Pulls values from the map, ordered by the keys in the sequence"
  [m keys]
  (reduce #(conj %1 (%2 m)) [] keys))


(defn map->row
  "Converts Clojure map to Spark Row using specified schema"
  [keys]
  (fn [data]
    (->> (select-values data keys)
         (into-array Object)
         (RowFactory/create))))


(defn vector->row
  "Converts Clojure sequence to Spark Row"
  [data]
  (->> (into-array Object data)
       (RowFactory/create)))


(defn data-frame->rdd-of-maps
  "Converts DataFrame to RDD of Clojure Maps"
  [data-frame]
  (let [schema (.schema data-frame)]
    (spark/map (row->map schema) (.toJavaRDD data-frame))))


(defn data-frame->rdd-of-vectors
  "Converts DataFrame to RDD of Vectors"
  [data-frame]
  (let [schema (.schema data-frame)]
    (spark/map (row->vector schema) (.toJavaRDD data-frame))))


(defn create-struct-field
  "Creates a StructField object from tuple of field name, type and nullable boolean"
  [field-params]
  (DataTypes/createStructField (name (first field-params))
                               ((keyword (second field-params)) schema-types)
                               (last field-params)))


(defn build-schema
  "Creates a StructType object, which represents a schema. `fields` should be a
  sequence of vectors containing the field name, the field type and a boolean
  indicating if the field is nullable"
  [fields]
  (let [struct-fields (into [] (map create-struct-field fields))]
    (DataTypes/createStructType (java.util.ArrayList. struct-fields))))


(defn rdd-of-maps->data-frame
  "Convert an RDD of Maps to a DataFrame"
  [spark-session rdd fields]
  (let [columns (map #(keyword (first %)) fields)
        schema  (build-schema fields)
        row-rdd (spark/map (map->row columns) rdd)]
    (.createDataFrame spark-session row-rdd schema)))


(defn rdd-of-vectors->data-frame
  "Convert an RDD of Vectors to a DataFrame"
  [spark-session rdd fields]
  (let [columns (map #(keyword (second %)) fields)
        schema  (build-schema fields)
        row-rdd (spark/map vector->row rdd)]
    (.createDataFrame spark-session row-rdd schema)))
