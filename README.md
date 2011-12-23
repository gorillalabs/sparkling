# About this repository #

This is a template repository for Clojurewerkz Clojure projects ([monger](https://github.com/michaelklishin/monger), [langohr](https://github.com/michaelklishin/langohr), [neocons](https://github.com/michaelklishin/neocons), [welle](https://github.com/michaelklishin/welle) and so on).
It is supposed to be cloned once and modified for particular project's needs after that.

## How to regenerate the site

In order to modify contents and launch dev environment, run:

      cd _source
      bundle install
      bundle exec nanoc autocompile

In order to recompile assets for publishing, run

      ./_source/compile.sh

## Supported liquid helpers:

In order to add example to your docs, use example helper. Pass relative path to file (it should be located at ./source/content/examples folder) and Gist ID.

  {% example working_with_queues/01b_declaring_a_queue_using_queue_constructor.rb, 998727 %}

In order to add link to YARD docs (relevant for Ruby projects), use yard_link helper, and YARD notation for object/method, for example:

  {% yard_link AMQP::Queue#unbind %}

## License & Copyright

Copyright (C) 2011 Alexander Petrov, Michael S. Klishin.

Distributed under the Eclipse Public License, the same as Clojure.
