(ns flambo.function-test
  (:require [flambo.function :as func]
            [flambo.kryoserializer :as ks]
            [clojure.test :refer :all]
            ))


(deftest serializable-functions

  (let [kryo (ks/kryo-serializer)
        myfn (fn [x] (* 2 x))]

    (testing
      "we can serialize and deserialize it to a function"
      (is (fn? (ks/round-trip kryo myfn))))

    (testing (is (= 6
                    ((ks/round-trip kryo myfn) 3))))


    (let [x {:f identity :g (fn [s] (identity s))}] ;; check discussion on https://github.com/yieldbot/flambo/issues/42
      (testing (is (= 'test-symbol
                      (((ks/round-trip kryo x) :g)
                       'test-symbol)
                      ))))))
