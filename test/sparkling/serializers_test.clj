(ns sparkling.serializers-test
  (:require [clojure.test :refer :all]
            [sparkling.serializers :refer :all]))


;; TODO make this a real test

#_
(do
(defn field
  "Access to private or protected field. field-name must be something Named

   class - the class where the field is declared
   field-name - Named
   obj - the instance object, or a Class for static fields"
  [class field-name obj]
  (-> class (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(let [registry (Kryo.)
      _ (.setRegistrationRequired registry true)
      _ (.registerClasses (pmd.pacs.utils.registrator.registrator.kryoExtendedRegistrator.) registry)
      _ (println "Strategy: " (field Kryo "strategy" registry))
      s-set (sorted-set 1 2 3)
      persisted-set (->> s-set
                         (buffer/write-bytes registry)
                         (buffer/read-bytes registry))]

  ;; Deserialize the instance from the buffer
  (println (= s-set persisted-set))
  (println (type persisted-set))
  (println persisted-set)
  (def r persisted-set)
  )

(let [registry (Kryo.)
      _ (.registerClasses (pmd.pacs.utils.registrator.registrator.kryoExtendedRegistrator.) registry)
      o-array (object-array [1 2 3])
      persisted-array (->> o-array
                           (buffer/write-bytes registry)
                           (buffer/read-bytes registry))]

  ;; Deserialize the instance from the buffer
  (println "Object Array ------")
  (println (= (into [] o-array) (into [] persisted-array)))
  (println (type o-array))
  (println o-array)
  (println (type persisted-array))
  (println persisted-array)
  ;(def r persisted-set)
  )

)