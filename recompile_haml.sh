#! /bin/sh
find . -name "*.haml" -type f -print | sed -e "p;s/\.haml$/\.html/" | xargs -n2 haml
find . -name "styles.sass" -type f -print | sed -e "p;s/\.sass$/\.css/" | xargs -n2 sass
