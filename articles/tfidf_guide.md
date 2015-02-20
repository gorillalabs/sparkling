---
title: "Computing TF-IDF with Spark and Clojure"
layout: article
description: "Tutorial showing a complete sample project using Gorillalabs Sparkling to compute Term Frequency / Inverse Document Frequency"
---


## About this guide

This guide assumes, that you have basic knowledge of Sparkling, e.g. by reading the [getting started guide](/sparkling/articles/getting_started.html). It will walk you through a complete (albeit small) project. The task fulfilled in the project will compute  [tf-idf](https://en.wikipedia.org/wiki/Tf-idf) for a set of documents. Tf-idf stands for Term-frequency / inverse document frequency and is used to determine relevance of a document in a given corpus for a given query. Thus, given a set of documents, our project will yield a term vector for each document.

The code for this guide can be found in the master branch of our ["getting started repo"](https://github.com/gorillalabs/sparkling-getting-started/). However, you should be able to setup the complete project simply by following this guide.

 * [Basic explanation of Tf-idf](#tf-idf)
 * [Setting up a project with leinigen](#new-project)
 * [(Insta)REPL-driven development](#REPL)
 * [Tests for your pure Clojure functions](#tests)
 * [Dealing with with Scalas `Tuple2` from Apache Spark](#tuple)
 * [Tests for your Spark units](#spark-tests)


 * [parallelizing data to a Spark Dataset](#rdds)
 * [reading data from a file into a Spark Dataset](#external)
 * [performing transformations on the Datasets](#rdd-operations)
 * [working with key-value-pairs](#keyvalue)
 * [calling actions on the Datasets.](#rdd-actions)


##<a name="tf-idf"/> What is tf-idf?

TF-IDF (term frequency-inverse document frequency) is a way to score the importance of terms in a document based on how frequently they appear across a collection of documents (corpus). The tf-idf weight of a term in a document is the product of its `tf` weight:

`tf(t, d) = (number of times term t appears in document d) / (total number of terms in document d)`

and its `idf` weight:

`idf(t) = ln(total number of documents in corpus / (1 + number of documents with term t))`

##<a name="new-project"/> Setting up a new project using lein

Make sure you have [Leiningen](http://leiningen.org/) installed, I'm currently using version 2.5.1.

{% highlight bash session %}
➜  tmp  lein version
  Leiningen 2.5.1 on Java 1.7.0_60 Java HotSpot(TM) 64-Bit Server VM
{% endhighlight %}


Now, create a new project named "tf-idf".

    ➜  tmp  lein new tf-idf
    Generating a project called tf-idf based on the 'default' template.
    The default template is intended for library projects, not applications.
    To see other templates (app, plugin, etc), try `lein help new`.

Now, open that project in your IDE of choice. Personally, I use IntelliJ/Cursive, but for the sake of this tutorial, let's use [Light Table](http://lighttable.com/), as a easy-going common denominator. We start by editing `project.clj`, adding a dependency to `[gorillalabs/sparkling 1.1.0]`.

The file should now look like this:

{% highlight clojure %}
(defproject tf-idf "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [gorillalabs/sparkling "1.1.0"]])
{% endhighlight %}

To check whether everything is fine run `lein test`, the result should look like this:

```Text

    ➜  tf-idf  lein test

    lein test tf-idf.core-test

    lein test :only tf-idf.core-test/a-test

    FAIL in (a-test) (core_test.clj:7)
    FIXME, I fail.
    expected: (= 0 1)
      actual: (not (= 0 1))

    Ran 1 tests containing 1 assertions.
    1 failures, 0 errors.
    Tests failed.
```




## <a name="REPL"/> (Insta)REPL-driven development

Now we can start working on our problem. Open `src/tf_idf/core.clj` in Light Table and turn it into an InstaREPL (using Ctrl+Space to open the Command palette, type "insta" and hit "enter"). You will see a little "live" tag in the upper right corner of you editor. Now delete the `foo` function definition and add a require statement to the namespace and our own functions, so the file looks like this:

``` Clojure
(ns tf-idf.core
  (:require [clojure.string :as string]))

(def stopwords #{"a" "all" "and" "any" "are" "is" "in" "of" "on"
                 "or" "our" "so" "this" "the" "that" "to" "we"})

(defn terms [content]
  (map string/lower-case (string/split content #" ")))

(def remove-stopwords (partial remove (partial contains? stopwords)))
```

These functions provide a basic domain model for our problem.

As we're in instarepl: just add

``` Clojure
(remove-stopwords (terms "a quick brown fox jumps"))
```
and see the functionality explode ;)

We'll add this as a test right now. So, stay tuned!

## <a name="tests"/> Unit tests for your big data processing
Ok, it's not quit big data right now, but a big pro for using Sparkling (or Spark, in general) is the ability to unit test the code.

Open `test/tf_idf/core_test.clj` in Light Table and replace the existing `a-test` with this:

``` Clojure
(deftest domain-functions-test
  (testing "domain functions"
    (is (= ["quick" "brown" "fox" "jumps"]
         (remove-stopwords (terms "A quick brown fox jumps"))
         ))))
```

I added a keybinding to my Light Table to execute tests on keystroke. You might want to have that also, because it makes things a lot easier here: Just open your keybindings (`Ctrl+space` to open the Commands, type `key`, select 'Settings: User keymaps') and add these lines inside the vector:

``` Clojure
[:editor "pmeta-t" (:eval.custom
                         "(clojure.test/run-all-tests
                            (re-pattern
                              (str \"^\" *ns* \".*\")))"
                         {:result-type :statusbar})]
```

Upon saving the `user.keymap` file, keybindings are reloaded and back in the `core_test.clj` editor tab I press cmd-t to run my test. This will give me info about my tests in the status bar.

For all others, just use


``` zsh
lein test
```

And yes, I know that we should test `remove-stopwords` and `terms` functions separtely, but this is just a tutorial, so I took the short route.


## <a name="tuple"/> Dealing with with Scalas `Tuple2` from Apache Spark

Go back to `core.clj`, as we're adding some more functionality.
For our next step we need to deal with some Spark internals: Spark uses Scalas `Tuple2` instances for the PairRDDs, i.e. for its key-value-semantics. Thus, we need to work with `Tuple2` also, but Sparkling makes this as easy as possible.

First, require `sparkling.core` namespace, so our namespace definition looks like this:

``` Clojure
(ns tf-idf.core
  (:require [clojure.string :as string]
            [sparkling.core :as spark]
            ))
```

Second, add another function

``` Clojure
(defn docid->term-tuples
  "Returns a stopword filtered seq of tuples of doc-id,[term term-frequency doc-terms-count]"
  [doc-id content]
  (let [terms (remove-stopwords
                (terms content))
        doc-terms-count (count terms)
        term-frequencies (frequencies terms)]
    (map (fn [term] (spark/tuple doc-id [term (term-frequencies term) doc-terms-count]))
         (distinct terms))))
```

This function can be tested in the InstaREPL by adding the following line at the end of `core.clj`:
``` Clojure
(docid->term-tuples "doc1" "A quick brown fox")
```

The InstaREPL unfolds the evaluation and shows as result

``` Clojure
(#sparkling/tuple ["doc1" ["quick" 1 3]] #sparkling/tuple ["doc1" ["brown" 1 3]] #sparkling/tuple ["doc1" ["fox" 1 3]])
```

So, doc1 has 1 appearance of "quick" from a total of 3 words. And yes, the count of words (doc-terms-count) is repeated in every term element.

As you can see, Sparkling brings its own tagged literal for the clojure reader to help you cope with `Tuple2` without interfering with Scala too much. `#sparkling/tuple` turns the following two-element vector into a `Tuple2` instance for you while reading. There's also a function for that: `sparkling.core/tuple`, referenced here with the namespace alias as `spark/tuple`.


<!--
Adding a dependency requires to reset the "connection" in Light Table. Got to Connections (Menu "Views" > "Connections") and "disconnect", switch off and on the "live" InstaREPL by clicking on the "live" tag in the upper right corner to re-connect.
-->


















## TO BE CONTINUED!












<!--

### Initializing Spark

flambo applications require a `SparkContext` object which tells Spark how to access a cluster. The `SparkContext` object requires a `SparkConf` object that encapsulates information about the application. We first build a spark configuration, `c`, then pass it to the flambo `spark-context` function which returns the requisite context object, `sc`:

```clojure
user=> (def c (-> (conf/spark-conf)
                  (conf/master master)
                  (conf/app-name "tfidf")
                  (conf/set "spark.akka.timeout" "300")
                  (conf/set conf)
                  (conf/set-executor-env env)))
user=> (def sc (f/spark-context c))
```

`master` is a special "local" string that tells Spark to run our app in local mode. `master` can be a Spark, Mesos or YARN cluster URL, or any one of the special strings to run in local mode (see [README.md](https://github.com/yieldbot/flambo/blob/develop/README.md#initializing-flambo) for formatting details).

The `app-name` flambo function is used to set the name of our application.

As with most distributed computing systems, Spark has a [myriad of properties](http://spark.apache.org/docs/latest/configuration.html) that control most application settings. With flambo you can either `set` these properties directly on a _SparkConf_ object, e.g., `(conf/set "spark.akka.timeout" "300")`, or via a Clojure map, `(conf/set conf)`. We set an empty map, `(def conf {})`, for illustration.

Similarly, we set the executor runtime enviroment properties either directly via key/value strings or by passing a Clojure map of key/value strings. `conf/set-executor-env` handles both.

### Computing TF-IDF

Our example will use the following corpus:

```clojure
user=> (def documents
        [["doc1" "Four score and seven years ago our fathers brought forth on this continent a new nation"]
         ["doc2" "conceived in Liberty and dedicated to the proposition that all men are created equal"]
         ["doc3" "Now we are engaged in a great civil war testing whether that nation or any nation so"]
         ["doc4" "conceived and so dedicated can long endure We are met on a great battlefield of that war"]])
```

where `doc#` is a unique document id.

We use the corpus and spark context to create a Spark [_resilient distributed dataset_](http://spark.apache.org/docs/latest/programming-guide.html#resilient-distributed-datasets-rdds) (RDD). There are two ways to create RDDs in flambo:

* _parallelizing_ an existing Clojure collection, as we'll do now:

```clojure
user=> (def doc-data (f/parallelize sc documents))
```

* [reading](https://github.com/yieldbot/flambo/blob/develop/README.md#external-datasets) a dataset from an external storage system

We are now ready to start applying [_actions_](https://github.com/yieldbot/flambo/blob/develop/README.md#rdd-actions) and [_transformations_](https://github.com/yieldbot/flambo/blob/develop/README.md#rdd-transformations) to our RDD; this is where flambo truly shines (or rather burns bright). It utilizes the powerful abstractions available in Clojure to reason about data. You can use Clojure constructs such as the threading macro `->` to chain sequences of operations and transformations.  

#### Term Frequency

To compute the term freqencies, we need a dictionary of the terms in each document filtered by a set of [_stopwords_](https://github.com/yieldbot/flambo/blob/develop/test/flambo/example/tfidf.clj#L10). We pass the RDD, `doc-data`, of `[doc-id content]` tuples to the flambo `flat-map` transformation to get a new stopword filtered RDD of `[doc-id term term-frequency doc-terms-count]` tuples. This is the dictionary for our corpus.

`flat-map` transforms the source RDD by passing each tuple through a function. It is similar to `map`, but the output is a collection of 0 or more items which is then flattened. We use the flambo named function macro `flambo.api/defsparkfn` to define our Clojure function `gen-docid-term-tuples`:

```clojure
user=> (defn gen-docid-term-tuples [doc-tuple]
         (let [[doc-id content] doc-tuple
               terms (filter #(not (contains? stopwords %))
                             (clojure.string/split content #" "))
               doc-terms-count (count terms)
               term-frequencies (frequencies terms)]
           (map (fn [term] [doc-id term (term-frequencies term) doc-terms-count])
                (distinct terms))))
user=> (def doc-term-seq (-> doc-data
                             (f/flat-map gen-docid-term-tuples)
                             f/cache))
```

Notice how we use pure Clojure in our Spark function definition to operate on and transform input parameters. We're able to filter stopwords, determine the number of terms per document and the term-frequencies for each document, all from within Clojure. Once the Spark function returns, `flat-map` serializes the results back to an RDD for the next action and transformation.

This is the raison d'être for flambo. It handles all of the underlying serializations to and from the various Spark Java types, so you only need to define the sequence of operations you would like to perform on your data. That's powerful.

Having constructed our dictionary we `f/cache` (or _persist_) the dataset in memory for future actions.

Recall term-freqency is defined as a function of the document id and term, `tf(document, term)`. At this point we have an RDD of *raw* term frequencies, but we need normalized term frequencies. We use the flambo inline anonymous function macro, `fn`, to define an anonymous Clojure function to normalize the frequencies and `map` our `doc-term-seq` RDD of `[doc-id term term-freq doc-terms-count]` tuples to an RDD of key/value, `[term [doc-id tf]]`, tuples. This new tuple format of the term-frequency RDD will be later used to `join` the inverse-document-frequency RDD and compute the final tfidf weights.

```clojure
user=> (def tf-by-doc (-> doc-term-seq
                          (f/map (fn [[doc-id term term-freq doc-terms-count]]
                                       [term [doc-id (double (/ term-freq doc-terms-count))]]))
                          f/cache))
```

Notice, again how we were easily able to use Clojure's destructuring facilities on the arguments of our inline function to name parameters.

As before, we cache the results for future actions.


#### Inverse Document Frequency

In order to compute the inverse document frequencies, we need the total number of documents:

```clojure
user=> (def num-docs (f/count doc-data))
```

and the number of documents that contain each term. The following step maps over the distinct `[doc-id term term-freq doc-terms-count]` tuples to count the documents associated with each term. This is combined with the total document count to get an RDD of `[term idf]` tuples:

```clojure
user=> (defn calc-idf [doc-count]
         (fn [[term tuple-seq]]
           (let [df (count tuple-seq)]
             [term (Math/log (/ doc-count (+ 1.0 df)))])))
user=> (def idf-by-term (-> doc-term-seq
                            (f/group-by (fn [[_ term _ _]] term))
                            (f/map (calc-idf num-docs))
                            f/cache))
```

#### TF-IDF

Now that we have both a term-frequency RDD of `[term [doc-id tf]]` tuples and an inverse-document-frequency RDD of `[term idf]` tuples, we perform the aforementioned `join` on the "terms" producing a new RDD of `[term [[doc-id tf] idf]]` tuples. Then, we `map` an inline Spark function to compute the tf-idf weight of each term per document returning our final RDD of `[doc-id term tf-idf]` tuples:

```clojure
user=> (def tfidf-by-term (-> (f/join tf-by-doc idf-by-term)
                              (f/map (fn [[term [[doc-id tf] idf]]]
                                           [doc-id term (* tf idf)]))
                              f/cache))
```

We cache the RDD for future actions.

Finally, to see the output of our example application we `collect` all the elements of our tf-idf RDD as a Clojure array, sort them by tf-idf weight, and for illustration print the top 10 to standard out:

```clojure
user=> (->> tfidf-by-term
            f/collect
            ((partial sort-by last >))
            (take 10)
            clojure.pprint/pprint)
(["doc2" "created" 0.09902102579427793]
 ["doc2" "men" 0.09902102579427793]
 ["doc2" "Liberty" 0.09902102579427793]
 ["doc2" "proposition" 0.09902102579427793]
 ["doc2" "equal" 0.09902102579427793]
 ["doc3" "civil" 0.07701635339554948]
 ["doc3" "Now" 0.07701635339554948]
 ["doc3" "testing" 0.07701635339554948]
 ["doc3" "engaged" 0.07701635339554948]
 ["doc3" "whether" 0.07701635339554948])
user=>
```

You can also save the results to a text file via the flambo `save-as-text-file` function, or an HDFS sequence file via `save-as-sequence-file`, but we'll leave those APIs for you to explore.

### Conclusion

And that's it, we're done! We hope you found this tutorial of the flambo API useful and informative.

flambo is being actively improved, so you can expect more features as Spark continues to grow and we continue to support it. We'd love to hear your feedback on flambo.
-->
