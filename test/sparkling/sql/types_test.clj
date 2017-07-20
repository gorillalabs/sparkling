(ns sparkling.sql.types-test
  (:require [clojure.java.io :as io]
            [sparkling.conf :as conf]
            [sparkling.core :as spark]
            [sparkling.sql :as sql]
            [sparkling.sql.types :as types]
            [clojure.test :refer :all])
  (:import [java.text SimpleDateFormat]
           [org.apache.spark.sql.catalyst.expressions GenericRowWithSchema]
           [scala.collection JavaConverters]
           [org.apache.spark.sql.types StructType StructField ]
           [java.sql Date Timestamp]))

(def schema
  [{:name      "name"
    :type      (types/string-type)
    :nullable? false}
   {:name      "age"
    :type      (types/long-type)
    :nullable? false}
   {:name      "likes-clojure?"
    :type      (types/boolean-type)
    :nullable? false}
   {:name      "attractiveness"
    :type      (types/double-type)
    :nullable? true}
   {:name      "dob"
    :type      (types/date-type)
    :nullable? false}
   {:name      "last-online"
    :type      (types/timestamp-type)
    :nullable? false}])

(def sample-data
  [{:name           "bob"
    :age            28
    :likes-clojure? true
    :attractiveness 23.4
    :dob            (Date. (.getTime #inst "1990-11-18"))
    :last-online    (Timestamp. (.getTime #inst "2015-10-12T20:18:12.231Z"))}])


(deftest convert-rdd->data-frame
  (let [conf (-> (conf/spark-conf)
                 (conf/master "local[*]")
                 (conf/app-name "spark sql select expr test"))]
    (spark/with-context sc conf
                        (let [struct-type (types/struct-type schema)
                              sqlc (sql/sql-context sc)

                              row (->>
                                    (spark/parallelize sc sample-data)
                                    (spark/map (juxt :name
                                                     :age
                                                     :likes-clojure?
                                                     :attractiveness
                                                     :dob
                                                     :last-online))
                                    (spark/map types/create-row)
                                    (sql/rdd->data-frame sqlc struct-type)
                                    (sql/selects ["name"
                                                  "age"
                                                  "likes-clojure?"
                                                  "attractiveness"
                                                  "dob"
                                                  "last-online"])

                                    (.collectAsList)
                                    (#(.get % 0)))]


                          (testing
                            "Check values"
                            (is (= "bob"
                                   (.getString row 0)))
                            (is (= 28
                                   (.getLong row 1)))
                            (is (= true
                                   (.getBoolean row 2)))
                            (is (= 23.4
                                   (.getDouble row 3)))
                            (is (= #inst "1990-11-18"
                                   (.getDate row 4)))
                            (is (= #inst "2015-10-12T20:18:12.231Z"
                                   (.getTimestamp row 5)))
                            )))))

(defn scalaseq->seq
  [scala-seq]
  (->> scala-seq
       (JavaConverters/seqAsJavaListConverter)
       (.asJava)
       (seq)))

(deftest schema->struct-type
  (let [[name-field
         age-field
         likes-clojure-field
         attractiveness-field
         dob-field
         last-online-field] (->> schema
                                 (types/struct-type)
                                 (scalaseq->seq))]
    (testing
      "name"
      (is (= (types/string-type)
             (.dataType name-field)))
      (is (= "name"
             (.name name-field)))
      (is (= false
             (.nullable name-field))))
    (testing
      "age"
      (is (= (types/long-type)
             (.dataType age-field)))
      (is (= "age"
             (.name age-field)))
      (is (= false
             (.nullable age-field))))
    (testing
      "likes-clojure?"
      (is (= (types/boolean-type)
             (.dataType likes-clojure-field)))
      (is (= "likes-clojure?"
             (.name likes-clojure-field)))
      (is (= false
             (.nullable likes-clojure-field))))
    (testing
      "attractiveness"
      (is (= (types/double-type)
             (.dataType attractiveness-field)))
      (is (= "attractiveness"
             (.name attractiveness-field)))
      (is (= true
             (.nullable attractiveness-field))))
    (testing
      "dob"
      (is (= (types/date-type)
             (.dataType dob-field)))
      (is (= "dob"
             (.name dob-field)))
      (is (= false
             (.nullable dob-field))))
    (testing
      "last-online"
      (is (= (types/timestamp-type)
             (.dataType last-online-field)))
      (is (= "last-online"
             (.name last-online-field)))
      (is (= false
             (.nullable last-online-field))))

    ))
