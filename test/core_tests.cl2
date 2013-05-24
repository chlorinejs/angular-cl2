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
