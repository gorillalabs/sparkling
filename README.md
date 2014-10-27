# Project Name Documentation

This is a documentation site for [Project Name](). Copy or clone me and adapt for your project
that needs documentation guides similar to [rubyamqp.info](http://rubyamqp.info), [clojuremongodb.info](http://clojuremongodb.info) and so on.


## Install Dependencies

With Bundler:

    bundle install --binstubs

### How to run a development server

    ./bin/jekyll serve --watch

then navigate to [localhost:4000](http://localhost:4000)

### How to regenerate the site

    ./bin/jekyll build

## With Docker

If you don't have ruby toolsets available on your machine, you can run this with Docker.

### Running a local development server

```
fig up
```

Then navigate to [localdocker:4000](http://localdocker:4000).

### Regenerating the site

```
fig run web jekyll build
```

## License & Copyright

Copyright (C) 2014 Alexander Petrov, Michael S. Klishin, Zack Maril.

Distributed under the Eclipse Public License, the same as Clojure.
