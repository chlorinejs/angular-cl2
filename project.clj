(defproject angular-cl2 "0.3.0-SNAPSHOT"
  :description "Angular macros for ChlorineJS (a subset of Clojure)"
  :url "http://github.com/chlorinejs/angular-cl2.git"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]]
  :node-dependencies []
  :nodejs {:keywords ["chlorinejs", "clojure",
                      "macro", "angular", "qunit"
                      "atom"]
           :devDependencies {:mocha ">= 1.7.4"}
           :scripts {:mocha "mocha -u qunit test/test_runner.js"
                     :mocha-auto "mocha -w -u qunit test/test_runner.js"}}
  :plugins [[lein-cl2c "0.0.1-SNAPSHOT"]
            [lein-npm "0.2.0"]]
  :cl2c {:compile
         {:watch ["src", "test"]
          :filter "src/"
          :path-map ["src/" => "lib/"]
          :paths ["node_modules/"]
          :strategy "prod"
          ;; some files may take too long to compile. We need a limit
          :timeout 2000
          }
         :dev
         {:watch ["src", "test"]
          :filter "test/test_runner.cl2"
          :paths ["node_modules/" "src/"]
          :strategy "dev"
          ;; some files may take too long to compile. We need a limit
          :timeout 2000
          }})
