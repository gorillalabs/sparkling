(ns sparkling.sql
  "spark sql api for clojure. As sparkling.core, pass the sql context as last parameter.
Read json or write json like sparkling.core/text-file or save-as-text-file."
  (:require [sparkling.function :as func])
  (:import [org.apache.spark.sql SQLContext]
           [org.apache.spark.sql functions]
           [com.google.common.collect ImmutableMap]))

(defn sql-context
  "create new SQLContext by spark context"
  [sc]
  (SQLContext. sc))

(defn sql
  "give sql script then return data frame"
  [code context]
  (.sql context code))

(defn register-temp-table
  "regist data frame as a tmp table named table-name and return data frame"
  [table-name data-frame]
  (.registerTempTable data-frame table-name)
  data-frame)

(defn select
  "DataFrame select, accept vector [column...]"
  [cols data-frame]
  (.select data-frame
           (into-array cols)))

(defn cols
  "create a vector for columns by given names"
  [names data-frame]
    (vec (map #(.col data-frame %) names)))

(defn selects
  "call select by given names"
  [columns data-frame]
  (.select data-frame
           (into-array (cols columns data-frame))))

(defn select-expr
  "call select expr by given column expressions"
  [expr data-frame]
  (.selectExpr data-frame
               (into-array expr)))

(defn where
  "call where by "
  [expression data-frame]
  (.where data-frame expression))

(defn join
  "DataFrame join use expression text"
  [expression other data-frame]
  (.join data-frame expression))

(defn join-on
  "data frame inner join use one column name"
  [column-name other data-frame]
  (.join data-frame other
         (.equalTo (.col data-frame column-name) (.col other column-name))))

(defn group-by
  "DataFrame group by"
  [cols data-frame]
  (.groupBy data-frame (into-array cols)))

(defn group-by-cols
  "DataFrame group by column names"
  [columns data-frame]
  (.groupBy data-frame (into-array (cols columns data-frame))))

(defn count
  "grouped data count"
  [cols grouped-data]
  (.max grouped-data (into-array cols)))

(defn max
  "grouped data max"
  [cols grouped-data]
  (.max grouped-data (into-array cols)))

(defn min
  "grouped min"
  [cols grouped-data]
  (.min grouped-data (into-array cols)))

(defn sum
  "grouped sum"
  [cols grouped-data]
  (.min grouped-data (into-array cols)))

(defn agg
  "dataset agg"
  [cols dataset]
  (.agg dataset (ImmutableMap/copyOf (apply hash-map cols))))

(defn with-column-renamed
  "DataFrame with column renamed."
  [exist-name new-name data-frame]
  (.withColumnRenamed data-frame exist-name new-name))

(defn order-by
  "封装 DataFrame 的 orderBy 操作"
  [cols data-frame]
  (.orderBy data-frame (into-array cols)))

(defn order-by-cols
  "order by columns named by the columns parameter"
  [columns data-frame]
  (.orderBy data-frame (into-array (cols columns data-frame))))

(defn register-udf1
  "register a user defined function match java api udf1 return SQLContext."
  [name func data-type sqlc]
  (-> sqlc
      .udf
      (.register name (func/function func) data-type))
  sqlc)

(defn register-udf2
  "register a user defined function match java api udf2 return SQLContext."
  [name func data-type sqlc]
  (-> sqlc
      .udf
      (.register name (func/function2 func) data-type))
  sqlc)

(defn json-rdd
  "convert data frame as json java rdd"
  [data-frame]
  (-> data-frame
      .toJSON
      .toJavaRDD))

(defn read-json
  "read json (file in path or a java rdd) as data-frame"
  [sqlc data-source]
  (-> sqlc
      .read
      (.json data-source)))

(defn write-json
  "save data-frame as json file to path"
  [data-source data-frame]
  (-> data-frame
      .write
      (.json data-source)))

(defn save-as-parquet
  "Write out data frame in parquet format"
  [data-frame path]
  (.. data-frame
      write
      (parquet path)))

(defn read-parquet
  "read folder that contains parquet data"
  [sql-context path]
  (.. sql-context
      read
      (load path)))

(defn show
  "print contents of data frame"
  [data-frame]
  (.show data-frame))

(defn print-schema
  "print schema of data frame"
  [data-frame]
  (.printSchema data-frame))
