(ns sparkling.session
  "Spark Session API for Clojure.  SparkSession is a unified entry point, combining
   SQLContext and HiveContext"
  (:require [sparkling.core :as spark])
  (:import [org.apache.spark.sql SparkSession SparkSession$Builder]
           [org.apache.spark.sql RowFactory]
           [org.apache.spark.sql.types DataTypes])
  (:gen-class))

(def schema-types {:binary DataTypes/BinaryType
                   :boolean DataTypes/BooleanType
                   :byte DataTypes/ByteType
                   :date DataTypes/DateType
                   :double DataTypes/DoubleType
                   :float DataTypes/FloatType
                   :integer DataTypes/IntegerType
                   :long DataTypes/LongType
                   :null DataTypes/NullType
                   :short DataTypes/ShortType
                   :string DataTypes/StringType
                   :timestamp DataTypes/TimestampType})

(defn spark-session
  "create a new SparkSession, re-using existing SparkContext if possible"
  [spark-conf]
  (-> (SparkSession/builder)
      (.config spark-conf)
      (.getOrCreate)))

(defn read-json
  "Reads a JSON data file, returning a DataFrame"
  [spark-session file-path]
  (-> spark-session
      (.read)
      (.json file-path)))

(defn read-parquet
  "Reads a Parquet data file, returning a DataFrame"
  [spark-session file-path]
  (-> spark-session
      (.read)
      (.parquet file-path)))

(defn write-parquet
  "Serializes a DataFrame as a Parquet file"
  [data-frame file-path]
  (-> data-frame
      (.write)
      (.format "parquet")
      (.save file-path)))

(defn print-schema
  "Print the schema of the DataFrame to standard out"
  [data-frame]
  (.printSchema data-frame))

(defn print-data
  "Prints the contents of a DataFrame to standard out"
  [data-frame]
  (.show data-frame))

(defn register-temp-table
  "Registers data frame as a temp table, making it queryable with SQL"
  [data-frame table-name]
  (.registerTempTable data-frame table-name)
  data-frame)

(defn run-query
  "Executes a SQL query against the current SparkSession"
  [spark-session sql-string]
  (.sql spark-session sql-string))

(defn row->map-fn
  "Converts Spark Row to Clojure map using specified schema"
  [schema]
  (let [fields (into [] (.fieldNames schema))
        offsets (range 0 (count fields))
        field-offsets (map vector fields offsets)]
    (fn [row]
      (->> (map #(vector (keyword (first %)) (.get row (second %))) field-offsets)
           (into {})))))

(defn row->vector-fn
  "Converst Spark Row to Clojure vector using specified schema"
  [schema]
  (let [fields (into [] (.fieldNames schema))
        offsets (range 0 (count fields))]
    (fn [row]
      (->> (map #(.get row %) offsets)
           (into [])))))

(defn select-values
  "Pulls values from the map, ordered by the keys in the sequence"
  [m keys]
  (reduce #(conj %1 (%2 m)) [] keys))

(defn map->row-fn
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
    (spark/map (row->map-fn schema) (.toJavaRDD data-frame))))

(defn data-frame->rdd-of-vectors
  "Converts DataFrame to RDD of Vectors"
  [data-frame]
  (let [schema (.schema data-frame)]
    (spark/map (row->vector-fn schema) (.toJavaRDD data-frame))))

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
        schema (build-schema fields)
        row-rdd (spark/map (map->row-fn columns) rdd)]
    (.createDataFrame spark-session row-rdd schema)))

(defn rdd-of-vectors->data-frame
  "Convert an RDD of Vectors to a DataFrame"
  [spark-session rdd fields]
  (let [columns (map #(keyword (second %)) fields)
        schema (build-schema fields)
        row-rdd (spark/map vector->row rdd)]
    (.createDataFrame spark-session row-rdd schema)))

