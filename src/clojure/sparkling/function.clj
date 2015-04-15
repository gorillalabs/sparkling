(ns sparkling.function
  )

(defmacro gen-function
  [clazz wrapper-name]

  `(defn ~wrapper-name [f#]
     (new ~(symbol (str "sparkling.function." clazz)) f#)))

(gen-function Function function)
(gen-function Function2 function2)
(gen-function Function3 function3)
(gen-function VoidFunction void-function)
(gen-function FlatMapFunction flat-map-function)
(gen-function FlatMapFunction2 flat-map-function2)
(gen-function PairFlatMapFunction pair-flat-map-function)
(gen-function PairFunction pair-function)
