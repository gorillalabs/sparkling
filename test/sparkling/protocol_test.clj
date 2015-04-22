(ns sparkling.protocol-test
  (:require [clojure.test :refer :all])
  (:import [java.io ObjectInputStream ByteArrayInputStream ObjectOutputStream ByteArrayOutputStream]))


(defn- serialize
  "Serializes a single object, returning a byte array."
  [v]
  (with-open [bout (ByteArrayOutputStream.)
              oos (ObjectOutputStream. bout)]
    (.writeObject oos v)
    (.flush oos)
    (.toByteArray bout)))

(defn- deserialize
  "Deserializes and returns a single object from the given byte array."
  [bytes]
  (with-open [ois (-> bytes ByteArrayInputStream. ObjectInputStream.)]
    (.readObject ois)))


(defprotocol timestamped
  (time-from-tweet [item]))

(defrecord tweet [username tweet timestamp]
  timestamped
  (time-from-tweet [_]
    timestamp
    ))

(defn tfn [x] (time-from-tweet x))

(defmulti tweettime type)

(defmethod tweettime tweet [item] (:timestamp item))

(deftest serialization1
  (testing "Serialization of function"
    (let [item identity]
      (is item (-> item serialize deserialize)))))


(deftest serialization4
  (testing "Serialization of protocol method"
    (let [item tfn]
      (is item (-> item serialize deserialize)))))