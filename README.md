Angular-cl2
===========
Angular macros for [ChlorineJS](https://github.com/chlorinejs/chlorine/wiki).

[![Build Status](https://travis-ci.org/chlorinejs/angular-cl2.png?branch=master)](https://travis-ci.org/chlorinejs/angular-cl2)

Comsume this package
--------------------
You need Java, NodeJS installed.

Pull angular-cl2 to your machine:
```
npm install angular-cl2
```
Now you should have `angular.cl2` somewhere in your `node_modules` directory. Include it as normal:
```clojure
(load-file "./path/or/url/to/angular.cl2")
;; define your Angular app now
(defmodule myApp ...)
```
A battery-included template is available at https://github.com/chlorinejs/angular-cl2-seed

Some other examples can be found at https://github.com/chlorinejs-demos/

Develope this package
---------------------

Install dependencies
--------------------

```
# install testem to run the tests on the fly
npm install
```

Live coding
-----------

Have your files watched and auto-compiled:
```
npm run-script watch
```
This will watch for changes and re-compile `*.cl2` files to Javascript.

Now open an other terminal, run testem:
```
npm run-script livetest
```

License
-------

Copyright © 2013 Hoang Minh Thang

Angular-cl2 library may be used under the terms of either the [GNU Lesser General Public License (LGPL)](http://www.gnu.org/copyleft/lesser.html) or the [Eclipse Public License (EPL)](http://www.eclipse.org/legal/epl-v10.html). As a recipient of angular-cl2, you may choose which license to receive the code under.
