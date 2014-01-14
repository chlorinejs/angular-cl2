# Angular-cl2

Angular macros for [ChlorineJS](https://github.com/chlorinejs/chlorine/wiki).

[![Build Status](https://travis-ci.org/chlorinejs/angular-cl2.png?branch=master)](https://travis-ci.org/chlorinejs/angular-cl2)

## Prerequisites

You will need [nodejs][1] and [Leiningen][2] 2.0 or above installed.

[1]: http://nodejs.org
[2]: http://leiningen.org

## Installation

Add angular-cl2 to your [lein-npm][3]-powered project:

[3]: https://github.com/bodil/lein-npm

```clojure
;; project.clj
:node-dependencies [angular-cl2 "0.3.0-SNAPSHOT"]
```
Pull angular-cl2 to your machine:
```
lein npm install
```
Now you should have `angular_cl2/src/core.cl2` in your `node_modules` directory.
Include it in your ChlorineJs source file(s):

```clojure
;; some-file.cl2
(load-file "angular_cl2/src/core.cl2")
```

## Develope angular-cl2

### Live coding

Have your files watched and auto-compiled:
```
lein cl2c auto dev
```
This will watch for changes and re-compile `*.cl2` files to Javascript.

Now open an other terminal, have mocha run the tests:
```
lein npm run-script mocha-auto
```

## Usage

FIXME

## License

Copyright © 2013 Hoang Minh Thang
Copyright © 2014 Hoang Minh Thang

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
