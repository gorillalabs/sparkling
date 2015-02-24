---
title: "Sparkling: all documentation guides"
layout: article
description: "A list of guides to Sparkling, the Clojure API to Apache Spark"
---

## Guide list

[Sparkling documentation](https://github.com/gorillalabs/sparkling/tree/gh-pages) is organized as a number of guides, covering all kinds of topics.

We recommend that you read these guides, if possible, in this order:

###  [Getting started](/sparkling/articles/getting_started.html)

This guide combines an overview of Sparkling with a quick tutorial that helps you to get started with it. It should take about 10 minutes to read and study the provided code examples. This guide covers:

 * Feature of Sparkling, why Sparkling was created
 * Clojure and Apache Spark version requirements
 * How to add Sparkling dependency to your project


###  [Fully working tutorial project](/sparkling/articles/tfidf_guide.html)

This guide takes you through a full project from setting it up with `lein new` to running it locally. Take an hour to complete. This guide covers:

 * Setting up a project, adding required dependencies
 * REPL-driven development, Test-driven development of both pure-Clojure functions and Spark transformations
 * Dealing with Spark's Tuple2 instances and destructuring them e.g. after `map-to-pair` or `join`.
 * Running your Spark powered application locally.
