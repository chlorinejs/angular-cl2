(deftest defroutetable-macro-tests
  (is (= (macroexpand
          (defroutetable
            "/an-url" [aCtrl "a-template-url.html"]
            "/alias" "/stuff"
            :default {:controller anOtherCtrl
                      :template (hiccup [:div "{{hello}}"])}))
         (macroexpand
          ["$routeProvider"
           (fn [$routeProvider]
             (.. $routeProvider
                 (when "/an-url"
                   {:controller aCtrl,
                    :templateUrl "a-template-url.html"})
                 (when "/alias"
                   {:redirectTo "/stuff"})
                 (otherwise
                  {:controller anOtherCtrl,
                   :template (hiccup [:div "{{hello}}"])}))
             nil)]))))

(deftest fn-di-macro-test
  (is (= (macroexpand
          (fn-di [foo bar] (+ foo bar)))
         (macroexpand
          ["foo" "bar"
           (fn [foo bar] (+ foo bar))]))))

(deftest def-something-macro-tests
  (is (= (macroexpand
          (def$ foo 1))
         (macroexpand
          (set! (. $scope -foo) 1))))
  (is (= (macroexpand
          (def!$ foo 1))
         (macroexpand
          (set! (.. this -$scope -foo) 1))))
  (is (= (macroexpand
          (defn$ foo [x] (+ 1 x)))
         (macroexpand
          (set! (. $scope -foo) (fn [x] (+ 1 x)))))))

(deftest defmodule-macro-tests
  (is (= (macroexpand
          (defmodule myApp
            (:route "foo" "bar")
            (:directive
             (myDirective
              []
              (fn [scope elm attrs])))
            (:service ;; :controller, :factory -> the same syntax
             (myService
              []
              (defn! addThree [n] (+ n 3))))
            (:filter (myFilter [] [x] (+ x 5))
                     (anOtherFilter [$http] [y] (+ y 6)))))
         (macroexpand-1
          (.. myApp
              (config (defroutetable "foo" "bar"))
              (directive "myDirective"
                         (fn-di [] (fn [scope elm attrs])))
              (service "myService"
                       (fn-di [] (defn! addThree [n] (+ n 3))))
              (filter "myFilter"
                      (fn-di [] (fn [x] (+ x 5))))
              (filter "anOtherFilter"
                      (fn-di [$http]
                             (fn [y] (+ y 6)))))))))

(deftest ng-test-macro-tests
  (is (= (macroexpand
          (ng-test myApp
            (:controller myCtrl
              (:tabular
               (addTwo 1) {:result 3}))

            (:service myService
              (:tabular
               (addThree 1) 4))

            (:filter myFilter
              (:tabular
               [1] 6))

            (:filter yourFilter
              (:tabular
               [2] 8))

            (:directive MyDirective
              (:tabular
               [:div {:my-directive "foo"}]
               {:foo 2}
               ;; Calling $compile function against provided template and scope
               ;; returns an element.
               ;; `(text)` (the same as `text` because they're called by `..` macro)
               ;; is method call of that element.
               ;; These methods are provided by Angular's jQuery lite
               ;; To get full list of them, consult `angular.element` section
               ;; in AngularJS Global APIs.
               "6" text
               "6" (text)))))
         (macroexpand
          (do
            (def injector (.. angular (injector ["ng" "myApp"])))
            (module "tests"
                    {:setup
                     (fn [] (set! (. this -$scope)
                                  (.. injector (get "$rootScope") $new)))})

            (deftest myCtrl
              (def $controller (.. injector (get "$controller")))
              ($controller "myCtrl" {:$scope (. this -$scope)})
              (equal (.. this -$scope (addTwo 1)) {:result 3}))

            (deftest myService
              (def myService (.. injector (get "myService")))
              (equal (.. myService (addThree 1)) 4))

            (deftest myFilter
              (def $filter (.. injector (get "$filter")))
              (equal (($filter "myFilter") 1) 6))

            (deftest yourFilter
              (def $filter (.. injector (get "$filter")))
              (equal (($filter "yourFilter") 2) 8))

            (deftest MyDirective
              (def $compile (.. injector (get "$compile")))
              (do
                (def element
                  (($compile
                    (hiccup [:div {:my-directive "foo"}]))
                   (. this -$scope)))
                (do (def!$ :foo 2)
                    (.. this -$scope $apply)
                    (equal "6" (.. element text))
                    (equal "6" (.. element (text)))
                    (delete (get* (. this -$scope) :foo)))))))))

  ;; Local Variables:
  ;; mode: clojure
  ;; eval: (define-clojure-indent
  ;;         (ng-test (quote defun))
  ;;         (:controller (quote defun))
  ;;         (:service (quote defun))
  ;;         (:filter (quote defun))
  ;;         (:directive (quote defun))
  ;;         (:factory (quote defun)))
  ;; End:
  )
