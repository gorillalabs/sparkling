---
title: "Computing TF-IDF with Spark and Clojure"
layout: article
description: "Tutorial showing a complete sample project using Gorillalabs Sparkling to compute Term Frequency / Inverse Document Frequency"
---

# Fully working tutorial project

## About this guide

This guide assumes, that you have basic knowledge of Sparkling, e.g. by reading the [getting started guide](/sparkling/articles/getting_started.html). It will walk you through a complete (albeit small) project. The task fulfilled in the project will compute  [tf-idf](https://en.wikipedia.org/wiki/Tf-idf) for a set of documents. Tf-idf stands for Term-frequency / inverse document frequency and is used to determine relevance of a document in a given corpus for a given query. Thus, given a set of documents, our project will yield a term vector for each document.

The code for this guide can be found in the master branch of our ["getting started repo"](https://github.com/gorillalabs/sparkling-getting-started/). However, you should be able to setup the complete project simply by following this guide.

 * [Basic explanation of Tf-idf](#tf-idf)
 * [Setting up a project with leinigen](#new-project)
 * [(Insta)REPL-driven development](#REPL)
 * [Tests for your pure Clojure functions](#tests)
 * [Dealing with with Scalas `Tuple2` from Apache Spark](#tuple)
 * [Going to the Spark Engine](#spark)
 * [Tests for your Spark units](#spark-tests)
 * [Destructuring Spark's objects using wrapper functions](#spark-destructuring)
 * [Spark transformations for idf and tf computation](#spark-transformation)
 * [Joining idf and tf information to get to tf-idf](#spark-joins)
 * [Run your Spark Application](#run-application)

##<a name="tf-idf"/> What is tf-idf?

TF-IDF (term frequency-inverse document frequency) is a way to score the importance of terms in a document based on how frequently they appear across a collection of documents (corpus). The tf-idf weight of a term in a document is the product of its `tf` weight:

`tf(t, d) = (number of times term t appears in document d) / (total number of terms in document d)`

and its `idf` weight:

`idf(t) = ln(total number of documents in corpus / (1 + number of documents with term t))`

##<a name="new-project"/> Setting up a new project using lein

Make sure you have [Leiningen](http://leiningen.org/) installed, I'm currently using version 2.5.1. And my working directory is `tmp`, as you can see from my shell prompt `➜  tmp `.

{% highlight text %}
➜  tmp  lein version
  Leiningen 2.5.1 on Java 1.7.0_60 Java HotSpot(TM) 64-Bit Server VM
{% endhighlight %}


Now, create a new project named "tf-idf".

{% highlight text %}
➜  tmp  lein new tf-idf
Generating a project called tf-idf based on the 'default' template.
The default template is intended for library projects, not applications.
To see other templates (app, plugin, etc), try `lein help new`.
{% endhighlight %}

Now, open that project in your IDE of choice. Personally, I use IntelliJ/Cursive, but for the sake of this tutorial, let's use [Light Table](http://lighttable.com/), as a easy-going common denominator. We start by editing `project.clj`, adding a dependency to `[gorillalabs/sparkling 1.1.1]`.

The file should now look like this:

{% highlight clojure %}
(defproject tf-idf "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [gorillalabs/sparkling "1.2.1"]]
  :aot [#".*" sparkling.serialization sparkling.destructuring]
  :profiles {:provided {:dependencies [[org.apache.spark/spark-core_2.10 "1.3.1"]]}}
  )
{% endhighlight %}

To check whether everything is fine run `lein test`, the result should look like this:

{% highlight text %}
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
{% endhighlight %}




## <a name="REPL"/> (Insta)REPL-driven development

Now we can start working on our problem. Open `src/tf_idf/core.clj` in Light Table and turn it into an InstaREPL (using Ctrl+Space to open the Command palette, type "insta" and hit "enter"). You will see a little "live" tag in the upper right corner of you editor. Now delete the `foo` function definition and add a require statement to the namespace and our own functions, so the file looks like this:

{% highlight clojure %}
(ns tf-idf.core
  (:require [clojure.string :as string]))

(def stopwords #{"a" "all" "and" "any" "are" "is" "in" "of" "on"
                 "or" "our" "so" "this" "the" "that" "to" "we"})

(defn terms [content]
  (map string/lower-case (string/split content #" ")))

(def remove-stopwords (partial remove (partial contains? stopwords)))
{% endhighlight %}


These functions provide a basic domain model for our problem.

As we're in instarepl: just add

{% highlight clojure %}
(remove-stopwords (terms "a quick brown fox jumps"))
{% endhighlight %}

and see the functionality explode ;)

We'll add this as a test right now. So, stay tuned!

## <a name="tests"/> Unit tests for your the code running inside your big data job
Ok, it's not quit big data right now, but a big pro for using Sparkling (or Spark, in general) is the ability to unit test the code. So far, we're not running on the Spark Engine, but we can test our Clojure code using the test framework of our choice. I usually prefer expectations, but for sake of simplicity, we're using clojure.test here.

Open `test/tf_idf/core_test.clj` in Light Table and replace the existing `a-test` with this:

{% highlight clojure %}
(deftest domain-functions-test
  (testing "tf functions"
    (is (= ["quick" "brown" "fox" "jumps"]
         (remove-stopwords (terms "A quick brown fox jumps"))
         ))))
{% endhighlight %}


I added a keybinding to my Light Table to execute tests on keystroke. You might want to have that also, because it makes things a lot easier here: Just open your keybindings (`Ctrl+space` to open the Commands, type `key`, select 'Settings: User keymaps') and add these lines inside the vector:

{% highlight clojure %}
[:editor "pmeta-t" (:eval.custom
                         "(clojure.test/run-all-tests
                            (re-pattern
                              (str \"^\" *ns* \".*\")))"
                         {:result-type :statusbar})]
{% endhighlight %}


Upon saving the `user.keymap` file, keybindings are reloaded and back in the `core_test.clj` editor tab I press cmd-t to run my test. This will give me info about my tests in the status bar.

For all others, just use


{% highlight text %}
lein test
{% endhighlight %}


And yes, I know that we should test `remove-stopwords` and `terms` functions separtely, but this is just a tutorial, so I took the short route.


## <a name="tuple"/> Dealing with with Scalas `Tuple2` from Apache Spark

Go back to `core.clj`, as we're adding some more functionality.
For our next step we need to deal with some Spark internals: Spark uses Scalas `Tuple2` instances for the PairRDDs, i.e. for its key-value-semantics. Thus, we need to work with `Tuple2` also, but Sparkling makes this as easy as possible.

First, require `sparkling.core` namespace, so our namespace definition looks like this:

{% highlight clojure %}
(ns tf-idf.core
  (:require [clojure.string :as string]
            [sparkling.core :as spark]
            ))
{% endhighlight %}


Second, add another function

{% highlight clojure %}
(defn term-count-from-doc
  "Returns a stopword filtered seq of tuples of doc-id,[term term-count doc-terms-count]"
  [doc-id content]
  (let [terms (remove-stopwords
               (terms content))
        doc-terms-count (count terms)
        term-count (frequencies terms)]
    (map (fn [term] (spark/tuple [doc-id term] [(term-count term) doc-terms-count]))
         (distinct terms))))
{% endhighlight %}


This function can be tested in the InstaREPL by adding the following line at the end of `core.clj`:
{% highlight clojure %}
(term-count-from-doc "doc1" "A quick brown fox")
{% endhighlight %}


The InstaREPL unfolds the evaluation and shows as result

{% highlight clojure %}
(#sparkling/tuple ["doc1" ["quick" 1 3]] #sparkling/tuple ["doc1" ["brown" 1 3]] #sparkling/tuple ["doc1" ["fox" 1 3]])
{% endhighlight %}


So, doc1 has 1 appearance of "quick" from a total of 3 words. And yes, the count of words (doc-terms-count) is repeated in every term element. This simplifies things by "wasting" some memory.

As you can see, Sparkling brings its own tagged literal for the clojure reader to help you cope with `Tuple2` without interfering with Scala too much. `#sparkling/tuple` turns the following two-element vector into a `Tuple2` instance for you while reading. There's also a function for that: `sparkling.core/tuple`, referenced here with the namespace alias as `spark/tuple`. Remember: The #sparkling/tuple tagged literal reads the rest of the expression as is, reading #sparkling.tuple[(int 3) :foo] will read a list (with elements `'int` and flaot 3) as key element. It will not evaluate the call to long you might have expected when writing this. Use the tuple function for this kind of stuff.


As we now have everything necessary for the tf-part of our computation, we still need the idf thing. Just add those two functions:

{% highlight clojure %}
(defn idf [doc-count doc-count-for-term]
  (Math/log (/ doc-count (+ 1.0 doc-count-for-term))))
{% endhighlight %}


And add a bunch of tests to your test namespace `core-test`. Just add the following testing-block to your existing deftest:

{% highlight clojure %}
(testing "idf functions"
    (is (= -0.8109302162163288 (idf 4 8)))
    (is (= -0.2231435513142097 (idf 4 4)))
    (is (= 1.3862943611198906 (idf 4 0))))
{% endhighlight %}

Run `lein test` from the command line to make sure everything works.


## <a name="spark"/> Going to the Spark Engine

We're ready to target the Spark engine to really process large amounts of data using a distributed system. .... Naaaa, we'll stay on a local Spark master for the moment, but that's ok for the sake of this guide: We're introducing the Spark Context and the RDDs required.

To deal with an annoyance I stumbled over from time to time, especially in demos given without network connectivity, we start with a little hack: Spark binds to an IP found on your system, but that might not be the one you're interested in. Thus, we configure that IP using the environment variable SPARK_LOCAL_IP. Good luck, there's a leiningen plugin for that. Open you `project.clj` and insert this `:dev` profile:

{% highlight clojure %}
:profiles {:dev {:plugins [[lein-dotenv "RELEASE"]]}}
{% endhighlight %}

And, while we're on it, also add `:aot :all` and a :main directive. If you're not sure - you're project.clj file should look like this:

{% highlight clojure %}
(defproject tf-idf "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [gorillalabs/sparkling "1.1.1"]
                 [org.apache.spark/spark-core_2.10 "1.2.1"]]
  :aot :all
  :main tf-idf.core
  :profiles {:dev {:plugins [[lein-dotenv "RELEASE"]]}})
{% endhighlight %}

The lein-dotenv plugin reads from an `.env` file and sets environment variables for the subsequent lein tasks. Thus, create a `.env` file in your project root directory with this content:

{% highlight bash %}
SPARK_LOCAL_IP=127.0.0.1
{% endhighlight %}

If you decided to not use Light Table but some other mechanism, make sure to make this environment variable available to your runtime.


Adding this requires you to reset the "connection" in Light Table. Got to Connections (Menu "Views" > "Connections") and "disconnect", start a new connection going to the editor tab for `core.clj`, open the command palette (Ctrl+Space) and select "Eval: Eval editor contents", which will start a new Clojure REPL connection for you.

In my Light Table version that's the point where InstaREPL is not working anymore, but evaluating the contents of the editor is perfectly possible. Remember the keystroke, you'll need it from time to time. For me it's `Cmd-Shift-Enter`.

Add the line
{% highlight clojure %}
(System/getenv "SPARK_LOCAL_IP")
{% endhighlight %}

to your `core.clj` file and evaluate. It should show "127.0.0.1" right next to the expression.

Now we're ready to create a Spark context.

First, require namespace `sparkling.conf`:
{% highlight clojure %}
(ns tf-idf.core
  (:require [clojure.string :as string]
            [sparkling.conf :as conf]
            [sparkling.core :as spark]))
{% endhighlight %}


Second, add this to `core.clj`:

{% highlight clojure %}
(defn make-spark-context []
  (let [c (-> (conf/spark-conf)
              (conf/master "local[*]")
              (conf/app-name "tfidf"))]
    (spark/spark-context c)))


(defn -main [& args]
  (let [sc (make-spark-context)]
    sc
    ))
{% endhighlight %}


Test this in your command line using `lein run` and you should see something like this:


{% highlight text %}
➜  tf-idf  lein run
Compiling tf-idf.core
...
15/02/23 11:14:02 INFO Utils: Successfully started service 'sparkDriver' on port 60023.
...
15/02/23 11:14:03 INFO Utils: Successfully started service 'HTTP file server' on port 60027.
15/02/23 11:14:04 INFO Utils: Successfully started service 'SparkUI' on port 4040.
15/02/23 11:14:04 INFO SparkUI: Started SparkUI at http://localhost:4040
15/02/23 11:14:04 INFO Executor: Starting executor ID <driver> on host localhost
...
➜  tf-idf  
{% endhighlight %}

As your program exits, the services are also teared down, so going to the Spark WebUI under http://localhost:4040 is pretty pointless right now. You won't find anything.

Remember: As Spark requires to have functions compiled, you need to AOT-compile the namespaces with functions used in spark transformations. This is done when running `lein run`, but it's not done in your REPL automatically. Strange errors? Do `lein do clean, run` and make sure nothing broke when you clean your stuff first.

##<a name="spark-tests"/> Tests for your Spark units

From here on, we go test driven. So open up `core_test.clj` and insert this in your ns definition:

{% highlight clojure %}
(:require [sparkling.core :as spark]
          [sparkling.conf :as conf]
          [sparkling.destructuring :as s-de])
{% endhighlight %}

as well as these functions (at the end of the file):

{% highlight clojure %}
(defn make-test-context []
  (-> (conf/spark-conf)
      (conf/master "local")
      (conf/app-name "tfidf-test")))

(def documents-fixture
  [(spark/tuple "doc1" "Four score and seven years ago our fathers brought forth on this continent a new nation")
   (spark/tuple "doc2" "conceived in Liberty and dedicated to the proposition that all men are created equal")
   (spark/tuple "doc3" "Now we are engaged in a great civil war testing whether that nation or any nation so")
   (spark/tuple "doc4" "conceived and so dedicated can long endure We are met on a great battlefield of that war")])
{% endhighlight %}

Both prepare stuff for our test: `make-test-context` creates a local, single-threaded Spark Context to use in our tests and the `documents-fixture` provides a tuple-set of documents with id and body as key/value pairs.

Now, we need the total number or documents to compute idf, so we start working on a `document-count` function in a TDD fashion. We add some tests down here:

{% highlight clojure %}
(deftest spark-functions-test
  (spark/with-context c (make-test-context)
    (testing "count the number of documents"
      (is (= 4
             (document-count
              (spark/parallelize-pairs c documents-fixture))))
      (is (= 0
             (document-count
              (spark/parallelize-pairs c [])))))
{% endhighlight %}

`lein test`ing that should result in an error, as we do not have a function document-count yet. So let's add it to `core.clj` just before the `-main` function:


{% highlight clojure %}
(defn document-count [documents]
  (spark/count documents))
{% endhighlight %}

Now, `lein test` should run 2 tests with 6 assertions and report back that everything is fine.

Congratulations, you wrote your first big data job using Sparkling, fully unit tested. ;) However, it just does nothing so far.

##<a name="spark-destructuring"/> Destructuring Spark's objects using wrapper functions

We now put the pieces together. From our RDD ([_resilient distributed dataset_](http://spark.apache.org/docs/latest/programming-guide.html#resilient-distributed-datasets-rdds)) of documents, we create another RDD using our `term-count-from-doc` function. We apply this function to every document using on of sparkling's map functions. As `term-count-from-doc` returns a collection of tuples for one document and we're interested in a collection of tuples over all documents, we're using `flat-map-to-pair`, stitching together all the different per-document collections to one for the whole corpus.

First, we create two small helper functions in our core_test.clj:

{% highlight clojure %}
(def tuple2vec (s-de/key-value-fn vector))
{% endhighlight %}

`tuple2vec` converts a Scala `Tuple2` instance returned from Spark to a vector using a wrapped version of the vector function. The `sparkling/destructuring` namespace contains a number of wrapper functions to destructure the different instances returned from Spark, depending on the situation. For example, a `(flat-)map-to-pair` function does return a `Tuple2` to store a key-value-pair. A join on two PairRDDs will return a `Tuple2` with another `Tuple2` as value, thus we need another wrapper function from `sparkling/destructuring` to wrap functions processing RDDs of that type.

{% highlight clojure %}
(defn first2vec [rdd]
  (tuple2vec (spark/first rdd)))
{% endhighlight %}

`first2vec` picks the first element from an RDD and returns it as a vector.


Second, we add a test for this inside the `spark-functions-test`:

{% highlight clojure %}
(testing "term-count-by-doc-term"
  (is (= [["doc1" "four"] [1 11]]
         (first2vec
          (term-count-by-doc-term
           (spark/parallelize-pairs c documents-fixture))))))
{% endhighlight %}

It's a bit oversimplifying to have this as the only test case, but it's enough to show how to test your sparkling code.


Now let's populate the `core.clj` to have your tests accepted:

Add `[sparkling.destructuring :as s-de]` as required to the namespace definition, and add this function (before the `-main` function).

{% highlight clojure %}
(defn term-count-by-doc-term [documents]
  (->>
   documents
   (spark/flat-map-to-pair
    (s-de/key-value-fn term-count-from-doc))
   spark/cache))
{% endhighlight %}

Now your tests should be green again. Just check.






##<a name="spark-transformation"/> Spark transformations for idf and tf computation

Next step is to compute idf. Remember, idf is defined like this:

`idf(t) = ln(total number of documents in corpus / (1 + number of documents with term t))`

We already have the total number of documents (`document-count`), thus we need the number of documents with term t for each term t in our corpus. Sounds like a PairRDD from term->document-count-for-term. We'll get this from our document-term-corpus like this:

Let's add that function:

{% highlight clojure %}
(defn document-count-by-term [document-term-count]
  (->> document-term-count
       (spark/map-to-pair (s-de/key-value-fn
                           (fn [[_ term] [_ _]] (spark/tuple term 1))))
       (spark/reduce-by-key +)))
{% endhighlight %}

This remaps the document-term-corpus to an rdd containing `term`,1-tuples, as each document/term-combination will appear only once in the document-term-corpus rdd. From there, we just need to count all documents per term.

I'll skip tests for that for sake of brevity, but you shouldn't do that. Go on, write a test for that in `core_test.clj`. I'll wait for you right here!


You're back again? That's fine!

Let's go on to compute the `idf-by-term easily:

{% highlight clojure %}
(defn idf-by-term [doc-count doc-count-for-term-rdd]
  (spark/map-values (partial idf doc-count) doc-count-for-term-rdd))
{% endhighlight %}


We do the same for `tf`. Remember tf's definition? Neither do I. So let's repeat:

`tf(t, d) = (number of times term t appears in document d) / (total number of terms in document d)`

Both numbers are in our term-count-from-doc-rdd for each document/term combination. So we map over this:

{% highlight clojure %}
(defn tf-by-doc-term [document-term-count]
  (spark/map-to-pair (s-de/key-value-fn
                      (fn [[doc term] [term-count doc-terms-count]]
                        (spark/tuple term [doc (/ term-count doc-terms-count)])))
                     document-term-count))
{% endhighlight %}



##<a name="spark-joins"/> Joining idf and tf information to get to tf-idf

To combine tf and idf, we need to join both RDDs. That's the reason we did return a `Tuple (term -> [doc tf])` from `tf-by-doc-term`, because Spark only joins over the same keys. So both `tf-by-doc-term` and `idf-by-term` need to be keyed by term.

With this said, it's easy to develop `tf-idf-by-doc-term`: First we join, and then we map-to-pair to a new doc,term combination as key and the tf*idf as value.

{% highlight clojure %}
(defn tf-idf-by-doc-term [doc-count document-term-count term-idf]
  (->> (spark/join (tf-by-doc-term document-term-count) term-idf)
       (spark/map-to-pair (s-de/key-val-val-fn
                   (fn [term [doc tf] idf]
                     (spark/tuple [doc term] (* tf idf)))))))
{% endhighlight %}

Did you enjoy following? Did you write tests for the new code? You could do so now, if you like!

Now, we just need to stich everything together like this:

{% highlight clojure %}
(defn tf-idf [corpus]
  (let [doc-count (document-count corpus)
        document-term-count (term-count-by-doc-term corpus)
        term-idf (idf-by-term doc-count (document-count-by-term document-term-count))]
      (tf-idf-by-doc-term doc-count document-term-count term-idf)))
{% endhighlight %}


##<a name="run-application"/> Run your Spark Application

Now populate your `-main` function like this:

{% highlight clojure %}
(defn -main [& args]
  (let [sc (make-spark-context)
        documents
        [(spark/tuple :doc1 "Four score and seven years ago our fathers brought forth on this continent a new nation")
         (spark/tuple :doc2 "conceived in Liberty and dedicated to the proposition that all men are created equal")
         (spark/tuple :doc3 "Now we are engaged in a great civil war testing whether that nation or any nation so")
         (spark/tuple :doc4 "conceived and so dedicated can long endure We are met on a great battlefield of that war")]
        corpus (spark/parallelize-pairs sc documents)
        tf-idf (tf-idf corpus)]
    (println (.toDebugString tf-idf))
    (clojure.pprint/pprint (spark/collect tf-idf))
    ))
{% endhighlight %}

There you are. `lein run` your project from the command line an you should see this:


{% highlight text %}
...
15/02/24 12:45:42 INFO Utils: Successfully started service 'sparkDriver' on port 53108.
...
15/02/24 12:45:43 INFO Executor: Starting executor ID <driver> on host localhost
...
15/02/24 12:45:44 INFO SparkContext: Starting job: count at NativeMethodAccessorImpl.java:-2
...
15/02/24 12:45:45 INFO DAGScheduler: Job 0 finished: count at NativeMethodAccessorImpl.java:-2, took 0,850507 s
...

(4) MappedRDD[9] at mapToPair at NativeMethodAccessorImpl.java:-2 []
 |  FlatMappedValuesRDD[8] at join at NativeMethodAccessorImpl.java:-2 []
 |  MappedValuesRDD[7] at join at NativeMethodAccessorImpl.java:-2 []
 |  CoGroupedRDD[6] at join at NativeMethodAccessorImpl.java:-2 []
 +-(4) MappedRDD[5] at mapToPair at NativeMethodAccessorImpl.java:-2 []
 |  |  FlatMappedRDD[1] at flatMapToPair at NativeMethodAccessorImpl.java:-2 []
 |  |  ParallelCollectionRDD[0] at parallelizePairs at NativeMethodAccessorImpl.java:-2 []
 |  MappedValuesRDD[4] at mapValues at NativeMethodAccessorImpl.java:-2 []
 |  ShuffledRDD[3] at reduceByKey at NativeMethodAccessorImpl.java:-2 []
 +-(4) MappedRDD[2] at mapToPair at NativeMethodAccessorImpl.java:-2 []
    |  FlatMappedRDD[1] at flatMapToPair at NativeMethodAccessorImpl.java:-2 []
    |  ParallelCollectionRDD[0] at parallelizePairs at NativeMethodAccessorImpl.java:-2 []

...
15/02/24 12:45:45 INFO SparkContext: Starting job: collect at NativeMethodAccessorImpl.java:-2
...
15/02/24 12:45:45 INFO DAGScheduler: Final stage: Stage 3(collect at NativeMethodAccessorImpl.java:-2)
15/02/24 12:45:45 INFO DAGScheduler: Parents of final stage: List(Stage 1, Stage 2)
15/02/24 12:45:45 INFO DAGScheduler: Missing parents: List(Stage 1, Stage 2)
15/02/24 12:45:45 INFO DAGScheduler: Submitting Stage 1 (MappedRDD[5] at mapToPair at NativeMethodAccessorImpl.java:-2), which has no missing parents
...
15/02/24 12:45:45 INFO DAGScheduler: Submitting 4 missing tasks from Stage 1 (MappedRDD[5] at mapToPair at NativeMethodAccessorImpl.java:-2)
...
15/02/24 12:45:45 INFO Executor: Running task 0.0 in stage 1.0 (TID 4)
...
15/02/24 12:45:46 INFO DAGScheduler: Stage 3 (collect at NativeMethodAccessorImpl.java:-2) finished in 0,303 s
15/02/24 12:45:46 INFO DAGScheduler: Job 1 finished: collect at NativeMethodAccessorImpl.java:-2, took 0,991440 s
[#sparkling/tuple [[:doc2 "created"] 0.09902102579427793] #sparkling/tuple [[:doc4 "long"] 0.07701635339554948] #sparkling/tuple [[:doc4 "can"] 0.07701635339554948] #sparkling/tuple [[:doc2 "conceived"] 0.041097438921682994] #sparkling/tuple [[:doc4 "conceived"] 0.03196467471686454] #sparkling/tuple [[:doc2 "equal"] 0.09902102579427793] #sparkling/tuple [[:doc3 "war"] 0.03196467471686454] #sparkling/tuple [[:doc4 "war"] 0.03196467471686454] #sparkling/tuple [[:doc3 "testing"] 0.07701635339554948] #sparkling/tuple [[:doc1 "continent"] 0.06301338005090412] #sparkling/tuple [[:doc1 "new"] 0.06301338005090412] #sparkling/tuple [[:doc4 "met"] 0.07701635339554948] #sparkling/tuple [[:doc1 "ago"] 0.06301338005090412] #sparkling/tuple [[:doc1 "forth"] 0.06301338005090412] #sparkling/tuple [[:doc1 "brought"] 0.06301338005090412] #sparkling/tuple [[:doc1 "seven"] 0.06301338005090412] #sparkling/tuple [[:doc4 "endure"] 0.07701635339554948] #sparkling/tuple [[:doc3 "great"] 0.03196467471686454] #sparkling/tuple [[:doc4 "great"] 0.03196467471686454] #sparkling/tuple [[:doc3 "whether"] 0.07701635339554948] #sparkling/tuple [[:doc1 "score"] 0.06301338005090412] #sparkling/tuple [[:doc2 "men"] 0.09902102579427793] #sparkling/tuple [[:doc4 "battlefield"] 0.07701635339554948] #sparkling/tuple [[:doc2 "proposition"] 0.09902102579427793] #sparkling/tuple [[:doc1 "years"] 0.06301338005090412] #sparkling/tuple [[:doc3 "now"] 0.07701635339554948] #sparkling/tuple [[:doc1 "four"] 0.06301338005090412] #sparkling/tuple [[:doc3 "civil"] 0.07701635339554948] #sparkling/tuple [[:doc2 "liberty"] 0.09902102579427793] #sparkling/tuple [[:doc1 "nation"] 0.026152915677434625] #sparkling/tuple [[:doc3 "nation"] 0.06392934943372908] #sparkling/tuple [[:doc1 "fathers"] 0.06301338005090412] #sparkling/tuple [[:doc2 "dedicated"] 0.041097438921682994] #sparkling/tuple [[:doc4 "dedicated"] 0.03196467471686454] #sparkling/tuple [[:doc3 "engaged"] 0.07701635339554948]]

{% endhighlight %}


And that's how you develop your own Spark job using Sparkling. From here, you can go on deploy your stuff to the cluster or run it from a local driver against the cluster - but beware the traps lying around there. To get you going until I find time to write a separate guide on that topic remember to use [`spark-submit`](https://spark.apache.org/docs/latest/submitting-applications.html), and to read the docs on [running Spark on a cluster](https://spark.apache.org/docs/latest/cluster-overview.html).

And you can checkout the whole source from our Github [getting-started project](https://github.com/gorillalabs/sparkling-getting-started/).
