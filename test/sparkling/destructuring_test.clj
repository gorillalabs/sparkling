(ns sparkling.destructuring-test
  (:use clojure.test)
  (:require [sparkling.destructuring :as sd]
            [sparkling.testutils :refer :all])
  (:import [scala Tuple2 Tuple3 Tuple10]
           [com.google.common.base Optional]))

(deftest destructuring
  (testing "destructures a Tuple2"
    (is (= (-> (Tuple2. 1 2)
               ((sd/fn [(k v)] [k v])))
           [1 2])))
  (testing "destructures a Tuple3"
    (is (= (-> (Tuple3. 1 2 3)
               ((sd/fn [(a b c)] [a b c])))
           [1 2 3])))
  (testing "destructures a Tuple10"
    (is (= (-> (Tuple10. 1 2 3 4 5 6 7 8 9 10)
               ((sd/fn [(a b c d e f g h i j)] [a b c d e f g h i j])))
           [1 2 3 4 5 6 7 8 9 10])))
  (testing "destructures nested tuples"
    (is (= (-> (Tuple2. (Tuple2. :a :b) (Tuple2. :c :d))
               ((sd/fn [((a b) (c d))] [a b c d])))
           [:a :b :c :d])))
  (testing "destructures a tuple with an Optional"
    (is (= (-> (Tuple3. 1 2 (Optional/of 3))
               ((sd/fn [(a b ?c)] [a b c])))
           [1 2 3])))
  (testing "destructures a tuple with an absent Optional"
    (is (= (-> (Tuple3. 1 2 (Optional/absent))
               ((sd/fn [(a b ?c)] [a b c])))
           [1 2 nil])))
  (testing "destructures a tuple with a seq"
    ;; there's no very obvious way to construct a scala.collection.convert.Wrappers$IterableWrapper without constructing a spark context
    ;; however, the implementation just calls clojure.core/seq on the argument
    (is (= (-> (Tuple3. 1 2 '(3 4 5))
               ((sd/fn [(a b -c)] [a b c])))
           [1 2 '(3 4 5)])))
  (testing "destructures a non-tuple seq"
    (is (= (-> '(3 4 5)
               ((sd/fn [-s] s)))
           '(3 4 5))))
  (testing "destructures a non-tuple Optional"
    (is (= (-> (Optional/of 1)
               ((sd/fn [?o] o)))
           1)))
  (testing "destructures multiple tuple args"
    (is (= ((sd/fn [(a b) (c d)]
              [a b c d])
            (Tuple2. 1 2)
            (Tuple2. 3 4))
           [1 2 3 4])))
  (testing "destructures a mix of tuples and clojure vectors"
    (is (= ((sd/fn [(a b) ([c d] [e f])]
              [a b c d e f])
            (Tuple2. 1 2)
            (Tuple2. [3 4] [5 6]))
           [1 2 3 4 5 6])))
  (testing "destructures clojure vectors nested inside tuples"
    (is (= ((sd/fn [(a [b c & d])]
              [a b c d])
            (Tuple2. 1 [2 3 4 5]))
           [1 2 3 '(4 5)])))
  (testing "destructures clojure maps nested inside tuples"
    (is (= ((sd/fn [(a {b :b c :c})]
              [a b c])
            (Tuple2. 1 {:b 2 :c 3}))
           [1 2 3]))
    (is (= ((sd/fn [(a {:keys [b c] :as amap})]
              [a b c amap])
            (Tuple2. 1 {:b 2 :c 3}))
           [1 2 3 {:b 2 :c 3}]))))
