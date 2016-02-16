## Using Spark's machine learning libraries in Clojure


Examples showing capabilities of the Clojure-SparkML wrapper.

Create the context and load the dataset.

```clojure
;Create the sqlcontext
(def sqc (mlcore/sql-context (s/local-spark-context "story")))
;;Download a libsvm format dataset from here:
;; "http://www.csie.ntu.edu.tw/~cjlin/libsvmtools/datasets/binary/svmguide1"
;;
;;NOTE the Spark only supports dataset labels in the range 0,1 (and not +1, -1)

;;load the dataset as a dataframe object
(def df (mlc/load-libsvm-dataset sqc "/tmp/svmguide1.txt"))

```


---


### Cross validation to get classification scores.

```clojure
;create a logistic regression classifier that uses default values
(def est (cl/logistic-regression))
;define the classifier (logistic-regression) and evaluator (binary classification evaluator)
;to use
(def eval-map {:evaluator (v/binary-classification-evaluator)
                :estimator est})

;create a cross validator with the logistic regression classifier
;as an estimator. Use a binary evaluator since there are 2 class labels
(def cv (v/cross-validator eval-map))

;get Cross validated scores
(v/avg-cv-metrics cv df)

```

Returns a sequence of CV scores, which is the cross-validated score for the area under ROC curve (the default metric)

```
(0.989250155930387)
```

___


### Train on training set and get  scores from validation set.

Using train/test validation split to get scores.
The default is 0.75/0.25 as train/validation split

```clojure
(def tv (v/train-val-split-validator eval-map))

;returns the scores from the validation set.
(v/tv-metrics tv df)

```

Returns the AUC-ROC score on a random 25% split taken as validation set.

```
(0.9903437278525866)
```

-----------------

### Grid search over classifier hyper-parameters

Set params to do grid search over best parameters.

```clojure

(let [ gsparams (v/param-grid [[(.regParam est) (double-array [0.1 0.05 0.01])]])
       ;;create the cross validator pipeline.
       cv (v/cross-validator (assoc eval-map
                             :estimator-param-maps gsparams))]
  (v/avg-cv-metrics cv df))

```

The result is a sequence corresponding to number of paramters to search over
```
(0.9296284637106029 0.9376697142571286 0.9422207222839514)`
```
Shows that 0.01 is the best value for the regularization parameter.

------------------


### Pipelines to transform and fit the data

Create pipelines which transform the features prior to training and validation

```clojure
(let [;scale the features to stay in the 0-1 range
      ss (xf/standard-scaler {:input-col "features"
                              :output-col "nfeatures"})

      ;tell the classifier to look for the modified features
      lr1 (doto (cl/logistic-regression) (.setFeaturesCol "nfeatures"))

      ;create a pipeline that scales the features first before training
      mlpipe (mlc/make-pipeline [ss lr1])

      ;validate using cross-validation as usual
      cv (v/cross-validator (assoc eval-map :estimator mlpipe))]
  (v/avg-cv-metrics cv df))
```

Returns a score of
```
(0.9892501559303871)
```
which shows that normalizing the data did not result in a improvement in the metric.

