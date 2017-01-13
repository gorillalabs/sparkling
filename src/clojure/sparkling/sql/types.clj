(ns sparkling.sql.types
  "allows for creation of dataframes from an existing rdd, steps:
  1. turn rdd into rows using create row
  2. create struct type passing in representation of the schema
  3. pass rdd of rows and struct type to sparkling.sql/data-frame"
  (:import [org.apache.spark.sql.types DataTypes]))

;The list of types supported by spark

(def primitive-types
  {:string            'DataTypes/StringType
   :binary            'DataTypes/BinaryType
   :boolean           'DataTypes/BooleanType
   :date              'DataTypes/DateType
   :timestamp         'DataTypes/TimestampType
   :calendar-interval 'DataTypes/CalendarIntervalType
   :double            'DataTypes/DoubleType
   :float             'DataTypes/FloatType
   :byte              'DataTypes/ByteType
   :integer           'DataTypes/IntegerType
   :long              'DataTypes/LongType
   :short             'DataTypes/ShortType
   :null              'DataTypes/NullType})

(defmacro def-primitive-data-types
  "Macro to generate functions to create the primitive DataTypes"
  []
  (let [def-all-types
        (->> primitive-types
             (map (fn [[t v]] (list 'defn (symbol (str (name t) "-type")) [] v)))
             (cons 'do))]
    def-all-types))

(def-primitive-data-types)

(defn decimal-type
  "create decimal type"
  [precision scale]
  (DataTypes/createDecimalType precision scale))

(defn array-type
  "create spark array type"
  ([element-type nullable?] (DataTypes/createArrayType element-type nullable?))
  ([element-type] (array-type element-type true)))

(defn map-type
  "create spark map type"
  ([key-type value-type value-nullable?] (DataTypes/createMapType key-type value-type value-nullable?))
  ([key-type value-type] (map-type key-type value-type true)))

(defn create-row
  "create row from a tuple of values, the values should all conform to a DataType.
   used for creating data frame from a rdd"
  [values]
  (RowFactory/create (into-array Object values)))

(defn- struct-field
  "struct field is sparks internal representation of a schema for one column/attribute"
  [{:keys [name type nullable?]}]
  (DataTypes/createStructField name type nullable?))

(defn struct-type
  "Creates a struct type, expect each item in fields to be a map with the keys :name :type and :optional?
  where name is a string, type is a DataType and optional is a boolean"
  [& fields]
  (DataTypes/createStructType (into-array (map struct-field fields))))
