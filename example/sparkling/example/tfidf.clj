(ns sparkling.example.tfidf
  (:require [sparkling.api :as s]
            [sparkling.destructuring :as sd]
            [sparkling.debug :as s-dbg]
            [sparkling.conf :as conf])
  (:gen-class))

(def master "local")
(def conf {})
(def env {})

(def stopwords #{"a" "all" "and" "any" "are" "is" "in" "of" "on"
                 "or" "our" "so" "this" "the" "that" "to" "we"})

;; Returns a stopword filtered seq of
;; [doc-id term term-frequency doc-terms-count] tuples
(defn gen-docid-term-tuples [doc-id content]
  (let [terms (filter #(not (contains? stopwords %))
                      (clojure.string/split content #" "))
        doc-terms-count (count terms)
        term-frequencies (frequencies terms)]
    (map (fn [term] (s/tuple doc-id [term (term-frequencies term) doc-terms-count]))
         (distinct terms))))

(defn calc-idf [doc-count]
  (fn [term tuple-seq]
    (let [df (count tuple-seq)]
      (s/tuple term (Math/log (/ doc-count (+ 1.0 df)))))))

(defn -main [& args]
  (try
    (let [c (-> (conf/spark-conf)
                (conf/master master)
                (conf/app-name "tfidf")
                (conf/set "spark.akka.timeout" "300")
                (conf/set conf)
                (conf/set-executor-env env))
          sc (s/spark-context c)

          ;; sample docs and terms
          documents [(s/tuple "doc1" "Four score and seven years ago our fathers brought forth on this continent a new nation")
                     (s/tuple "doc2" "conceived in Liberty and dedicated to the proposition that all men are created equal")
                     (s/tuple "doc3" "Now we are engaged in a great civil war testing whether that nation or any nation so")
                     (s/tuple "doc4" "conceived and so dedicated can long endure We are met on a great battlefield of that war")]

          doc-data (s/parallelize-pairs sc documents)
          _ (s-dbg/inspect doc-data "doc-data")

          ;; stopword filtered RDD of [doc-id term term-freq doc-terms-count] tuples
          doc-term-seq (-> doc-data
                           (s/flat-map-to-pair (sd/key-value-fn gen-docid-term-tuples))
                           (s-dbg/inspect "doc-term-seq")
                           s/cache)

          ;; RDD of term-frequency tuples: [term [doc-id tf]]
          ;; where tf is per document, that is, tf(term, document)
          tf-by-doc (-> doc-term-seq
                        (s/map-to-pair (sd/key-value-fn (fn [doc-id [term term-freq doc-terms-count]]
                                     (s/tuple term [doc-id (double (/ term-freq doc-terms-count))]))))
                        (s-dbg/inspect "tf-by-doc")
                        s/cache)

          ;; total number of documents in corpus
          num-docs (s/count doc-data)

          ;; idf of terms, that is, idf(term)
          idf-by-term (-> doc-term-seq
                          (s/group-by (sd/key-value-fn (fn [_ [term _ _]] term)))
                          (s-dbg/inspect "in idf-by-term (1)")
                          (s/map-to-pair (sd/key-value-fn (calc-idf num-docs)))
                          (s-dbg/inspect "in idf-by-term (2)")
                          s/cache)

          ;; tf-idf of terms, that is, tf(term, document) x idf(term)
          tfidf-by-term (-> (s/join tf-by-doc idf-by-term)
                            (s-dbg/inspect "in tfidf-by-term (1)")
                            (s/map (sd/key-val-val-fn (fn [term [doc-id tf] idf]
                                         [doc-id term (* tf idf)])))
                            (s-dbg/inspect "in tfidf-by-term (2)")
                            s/cache)
          ]
      (->> tfidf-by-term
           s/collect
           ((partial sort-by last >))
           (take 10)
           clojure.pprint/pprint))
    (catch Exception e
      (println (.printStackTrace e)))))
