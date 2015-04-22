(ns sparkling.function-test
  (:require [sparkling.function :as func]
            [sparkling.serialization :as ser]               ;; required for generating Registrator in conf.
            [sparkling.kryoserializer :as ks]
            [clojure.test :refer :all]
            [sparkling.conf :as conf])
  (:import [sparkling.serialization AbstractSerializableWrappedIFn]
           [com.esotericsoftware.kryo Kryo]))


(deftest serializable-functions

  (let [conf (-> (conf/spark-conf)
                 (conf/set-sparkling-registrator)
                 (conf/set "spark.kryo.registrationRequired" "false")
                 (conf/master "local[*]")
                 (conf/app-name "api-test"))
        #^Kryo kryo (ks/kryo-serializer conf)
        myfn (fn [x] (* 2 x)) #_(sparkling.function/function (fn [x] (* 2 x)))]


    (testing
      "we can serialize and deserialize it to a function"
      (is (fn? (ks/round-trip kryo myfn))))

    (testing (is (= 6
                    ((ks/round-trip kryo myfn) 3))))


    (let [x {:f identity :g (fn [s] (identity s))}] ;; check discussion on https://github.com/yieldbot/sparkling/issues/42
      (testing (is (= 'test-symbol
                      (((ks/round-trip kryo x) :g)
                       'test-symbol)
                      ))))))
