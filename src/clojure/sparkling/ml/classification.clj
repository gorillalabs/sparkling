(ns sparkling.ml.classification
  (:import  [org.apache.spark.ml.classification NaiveBayes LogisticRegression
             DecisionTreeClassifier RandomForestClassifier GBTClassifier ]))

;;defines wrappers for classifiers

(defn logistic-regression
  "Return a instance of LogisticRegression classifier.
  arguments are hyperparameters used in training the model.
  When no arguments are supplied, the default values (specified in the scala
  implementation are used."
  ([] (logistic-regression {}))
  ([{:keys [  max-iter  elastic-net-param
              fit-intercept reg-param
              standardization threshold
              thresholds tol
              weight-col ] }]
   (let [lr (LogisticRegression.)]
     (when max-iter (.setMaxIter lr max-iter))
     (when elastic-net-param (.setElasticNetParam lr elastic-net-param))
     (when fit-intercept (.setFitIntercept lr fit-intercept))
     (when reg-param (.setRegParam lr reg-param))
     (when standardization (.setStandardization lr standardization ))
     (when threshold (.setThreshold lr threshold))
     (when thresholds (.setThresholds lr thresholds))
     (when tol (.setTol lr tol))
     (when weight-col (.setWeightCol lr weight-col))
     lr)))

(defn naive-bayes
  "Return a instance of NaiveBayes classifier.
  arguments are hyperparameters used in training the model.
  When no arguments are supplied, the default values (specified in the scala
  implementation are used."
  ([] (naive-bayes {}))
  ([{:keys [  model-type smoothing ] }]
   (let [nb (NaiveBayes.)]
     (when model-type (.setModelType nb model-type))
     (when smoothing (.setSmoothing nb smoothing))
     nb)))

;;Decisision Tree, RandomForest and Gradient Boosted Trees
;;will not work the same way as logisticRegression, unless
;;the target variable/column is set in a specific manner.
;;At least until this issue SPARK-7126
;;(https://issues.apache.org/jira/browse/SPARK-7126)
;;is fixed
(defn decisiontree-classifier
  "Returns a decision tree classifier. "
  ([] (decisiontree-classifier {}))
  ([{:keys [cache-node-ids checkpoint-interval
            impurity max-bins
            max-depth max-memory-in-mb
            min-info-gain min-instances-per-node
            seed]}]
   (let [dtc (DecisionTreeClassifier.)]
     (when cache-node-ids (.setCacheNodeIds dtc cache-node-ids))
     (when checkpoint-interval (.setCheckpointInterval dtc checkpoint-interval))
     (when impurity (.setImpurity dtc impurity))
     (when max-bins (.setMaxBins dtc max-bins))
     (when max-depth (.setMaxDepth dtc max-depth))
     (when max-memory-in-mb (.setMaxMemoryInMB dtc max-memory-in-mb))
     (when  min-info-gain (.setMinInfoGain dtc min-info-gain))
     (when min-instances-per-node (.setMinInstancesPerNode dtc min-instances-per-node))
     (when seed (.setSeed dtc seed))
     dtc)))

(defn randomforest-classifier
  "Returns a randomforest classifier. "
  ([] (randomforest-classifier {}))
  ([{:keys [cache-node-ids checkpoint-interval
            feature-subset-strategy
            impurity max-bins
            max-depth max-memory-in-mb
            min-info-gain min-instances-per-node
            num-trees seed
            subsampling-rate]}]
   (let [rfc (RandomForestClassifier.)]
     (when cache-node-ids (.setCacheNodeIds rfc cache-node-ids))
     (when checkpoint-interval (.setCheckpointInterval rfc checkpoint-interval))
     (when feature-subset-strategy (.setFeatureSubsetStrategy rfc feature-subset-strategy))
     (when impurity (.setImpurity rfc impurity))
     (when max-bins (.setMaxBins rfc max-bins))
     (when max-depth (.setMaxDepth rfc max-depth))
     (when max-memory-in-mb (.setMaxMemoryInMB rfc max-memory-in-mb))
     (when min-info-gain (.setMinInfoGain rfc min-info-gain))
     (when min-instances-per-node (.setMinInstancesPerNode rfc min-instances-per-node))
     (when num-trees (.setNumTrees rfc num-trees))
     (when seed (.setSeed rfc seed))
     (when subsampling-rate (.setSubsamplingRate rfc subsampling-rate))
     rfc)))

(defn gbt-classifier
  "Returns a Gradient Boosted tree classifier "
  ([] (gbt-classifier {}))
  ([{:keys [cache-node-ids checkpoint-interval
            impurity loss-type
            max-bins
            max-depth max-iter
            max-memory-in-mb
            min-info-gain min-instances-per-node
            seed step-size
            subsampling-rate]}]
   (let [gbc (GBTClassifier.)]
     (when cache-node-ids (.setCacheNodeIds gbc cache-node-ids))
     (when checkpoint-interval (.setCheckpointInterval gbc checkpoint-interval))
     (when impurity (.setImpurity gbc impurity))
     (when loss-type (.setLossType gbc loss-type))
     (when max-bins (.setMaxBins gbc max-bins))
     (when max-depth (.setMaxDepth gbc max-depth))
     (when max-iter (.setMaxIter gbc max-iter))
     (when max-memory-in-mb (.setMaxMemoryInMB gbc max-memory-in-mb))
     (when min-info-gain (.setMinInfoGain gbc min-info-gain))
     (when min-instances-per-node (.setMinInstancesPerNode gbc min-instances-per-node))
     (when step-size (.setStepSize gbc step-size))
     (when seed (.setSeed gbc seed))
     (when subsampling-rate (.setSubsamplingRate gbc subsampling-rate))
     gbc)))
