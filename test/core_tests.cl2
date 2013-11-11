(deftest defroutetable-macro-tests
  (is (= (macroexpand
          (defroutetable
            "/an-url" [a-ctrl "a-template-url.html"]
            "/alias" "/stuff"
            :default {:controller an-other-ctrl
                      :template (hiccup [:div "{{hello}}"])}))
         (macroexpand
          [:$route-provider
           (fn [$route-provider]
             (.. $route-provider
                 (when "/an-url"
                   {:controller a-ctrl,
                    :templateUrl "a-template-url.html"})
                 (when "/alias"
                   {:redirectTo "/stuff"})
                 (otherwise
                  {:controller an-other-ctrl
                   :template (hiccup [:div "{{hello}}"])}))
             nil)]))))

(deftest fn-di-&-defn-di-macro-tests
  (is (= (macroexpand
          (defn-di foo
            [] (+ 1 2)))
         (macroexpand
          (def foo (fn [] (+ 1 2))))))
  (is (= (macroexpand
          (defn-di foo
            [bar boo] (+ bar boo)))
         (macroexpand
          (def foo
            [:bar :boo
             (fn [bar boo] (+ bar boo))]))))
  (is (= (macroexpand
          (defn-di foo.bar
            [bar boo] (+ bar boo)))
         (macroexpand
          (set! foo.bar
                [:bar :boo
                 (fn [bar boo] (+ bar boo))]))))
  (is (= (macroexpand
          (fn-di [] (+ 1 2)))
         (macroexpand
          (fn [] (+ 1 2)))))
  (is (= (macroexpand
          (fn-di [foo bar] (+ foo bar)))
         (macroexpand
          [:foo :bar
           (fn [foo bar] (+ foo bar))]))))

(deftest $-def$-defn$-macro-tests
  (is (= (macroexpand
          ($- foo.bar))
         (macroexpand
          $scope.foo.bar)))
  (is (= (macroexpand
          (def$ foo-bar 1))
         (macroexpand
          (set! $scope.foo-bar 1))))
  (is (= (macroexpand
          (defn$ foo-fun [x] (+ 1 x)))
         (macroexpand
          (set! $scope.foo-fun (fn [x] (+ 1 x)))))))

(deftest !-_tests
  (def that this)
  (this->!)
  (set! this.foo 3)
  ;; wraps in a function to change `this`
  (def! bar 5)
  (#(def! bazz 7))
  (is (= 3 that.foo))
  (is (= 5 that.bar))
  (is (= 7 that.bazz))
  (is (= 3 (!- foo)))
  (is (= 5 (!- bar)))
  (is (= 7 (!- bazz))))

(deftest defmodule-macro-tests
  (is (= (macroexpand
          (defmodule my-app
            (:route "foo" "bar")
            (:directive
              (my-directive
               []
               (fn [scope elm attrs])))
            (:service ;; :controller, :factory -> the same syntax
              (my-service
               []
               (defn! add-three [n] (+ n 3))))
            (:filter (my-filter [] [x] (+ x 5))
              (an-other-filter [$http] [y] (+ y 6)))))
         (macroexpand
          (.. my-app
              (config (defroutetable "foo" "bar"))
              (directive :my-directive
                         (fn-di [] (fn [scope elm attrs])))
              (service :my-service
                       (fn-di [] (defn! add-three [n] (+ n 3))))
              (filter :my-filter
                      (fn-di [] (fn [x] (+ x 5))))
              (filter :an-other-filter
                      (fn-di [$http]
                             (fn [y] (+ y 6)))))))))

(deftest ng-test-macro-tests
  (is (= (macroexpand
          (ng-test my-app
            (:controller my-ctrl
              (:tabular
               (add-two 1) {:result 3}))

            (:service my-service
              (:tabular
               (add-three 1) 4))

            (:filter my-filter
              (:tabular
               [1] 6))

            (:filter your-filter
              (:tabular
               [2] 8))

            (:directive my-directive
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
            (def injector (.. angular (injector [:ng :my-app])))
            (module "tests"
                    {:setup
                     (fn [] (set! (. this -$scope)
                                  (.. injector (get :$rootScope) $new)))})

            (deftest my-ctrl
              (def $controller (.. injector (get :$controller)))
              ($controller :my-ctrl {:$scope (. this -$scope)})
              (equal (.. this -$scope (add-two 1)) {:result 3}))

            (deftest my-service
              (def my-service (.. injector (get :my-service)))
              (equal (.. my-service (add-three 1)) 4))

            (deftest my-filter
              (def $filter (.. injector (get :$filter)))
              (equal (($filter :my-filter) 1) 6))

            (deftest your-filter
              (def $filter (.. injector (get :$filter)))
              (equal (($filter :your-filter) 2) 8))

            (deftest my-directive
              (def $compile (.. injector (get :$compile)))
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
