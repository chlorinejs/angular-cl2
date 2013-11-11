(defmacro defmodule
  [module-dclrs & section-dclrs]
  (let [final-module (if (symbol? module-dclrs)
                       module-dclrs
                       (let [[module-name module-deps] module-dclrs]
                         `(.. angular (module ~(keyword module-name)
                                              ~(mapv keyword module-deps)))))
        final-body
        (apply
         concat
         (for [section section-dclrs]
           (if (keyword? (first section))
             (let [[section-type & section-exprs] section]
               (if (= :route section-type)
                 [`(config (defroutetable ~@section-exprs))]
                 (for [section-expr section-exprs]
                   (cond
                    (= :filter section-type)
                    (let [[filter-name filter-deps & filter-body]
                          section-expr]
                      `(filter ~(keyword filter-name)
                               (fn-di ~filter-deps
                                      (fn ~@filter-body))))

                    (= :directive section-type)
                    (let [[d-name d-deps directive-def]
                          section-expr]
                      `(directive
                        ~(keyword d-name)
                        (fn-di ~d-deps ~directive-def)))

                    (contains? #{:controller :factory :service}
                               section-type)
                    (let [[di-name & body]
                          section-expr]
                      `(~(symbol (name section-type)) ~(keyword di-name)
                        (fn-di ~@body)))
                    :default
                    (throw (Exception. "Unsupported syntax"))))))
             [section])))]
    (if (zero? (count final-body))
      final-module
      `(..
        ~final-module
        ~@final-body))))

(defmacro defroutetable
  "Defines a route form. Usage:
  (defroutetable
    \"/an-url\" [myCtrl \"an-template-url.html\"]
    \"/alias\"  \"/stuff\"
   :default {:some :config :map ...})"
  [& routes]
  `(fn-di [$route-provider]
          (.. $route-provider
              ~@(for [[route route-config] (partition 2 routes)]
                  (let [head (if (keyword? route)
                               ['otherwise]
                               `(when ~route))
                        tail (cond
                              (vector? route-config)
                              {:controller (first route-config)
                               :templateUrl (second route-config)}

                              (string? route-config)
                              {:redirectTo route-config}

                              :default
                              route-config)]
                    `(~@head ~tail))))
          nil))

(defmacro fn-di
  "Like `fn` but will automatically generate dependency injection vectors.
  Doesn't support multi-arity."
  [& body]
  (let [[docstring v] (if (string? (first body))
                        [(first body) (second body)]
                        [nil (first body)])]
    (if (= v [])
      `(fn ~@body)
      (vec (concat (map #(keyword (name %)) v) [`(fn ~@body)])))))

(defmacro defn-di
  "Like `defn` but will automatically generate dependency injection vectors.
  Doesn't support multi-arity."
  [di-name & body]
  (let [[docstring v] (if (string? (first body))
                        [(first body) (second body)]
                        [nil (first body)])
        setter (if (contains? (set (seq (name di-name))) \.)
                 'set!
                 'def)]
    `(~setter ~di-name
       ~(if (= v [])
          `(fn ~@body)
          `[~@(map #(keyword (name %)) v) (fn ~@body)]))))

(defmacro ng-test
  [module-name & body]
  (let [final-body
        (for [expr body]
          (let [[test-type test-name & test-body] expr
                test-init
                (fn [test-type]
                  (cond
                   (= :controller test-type)
                   (list
                    `(def $controller
                       (.. injector (get :$controller)))
                    `($controller ~(keyword test-name)
                                  {:$scope (. this -$scope)}))

                   (= :service test-type)
                   (list
                    `(def ~test-name (.. injector (get ~(keyword test-name)))))

                   (= :filter test-type)
                   (list
                    `(def $filter (.. injector (get :$filter))))

                   (= :directive test-type)
                   (list
                    `(def $compile (.. injector (get :$compile))))

                   :default
                   nil))
                expand-tabular
                (fn [expr]
                  (if (= :directive test-type)
                    (let [[hiccup-template scope-map & test-table]
                          (rest expr)]
                      (list
                       `(do
                          (def
                            element
                            (($compile (hiccup
                                        ~hiccup-template))
                             (. this -$scope)))
                          ~(apply
                            concat
                            (for [[scope-var scope-val] scope-map]
                              `(do (set!
                                    ~(symbol (str "this.$scope."
                                                  (name scope-var)))
                                         ~scope-val)
                                   (.. this -$scope $apply)
                                   ~@(for [[expect-val test-method]
                                           (partition 2 test-table)]
                                       `(equal ~expect-val
                                               (.. element ~test-method)))
                                   (delete
                                    (get* (. this -$scope)
                                          ~scope-var)))
                              )))))
                    ;; test-types other than :directive
                    (for [[test-case expect-val]
                          (partition 2 (rest expr))]
                      (cond
                       (= :controller test-type)
                       `(equal ((~(keyword (first test-case))
                                 this.$scope)
                                ~@(rest test-case))
                               ~expect-val)
                       (= :service test-type)
                       `(equal ((~(keyword (first test-case))
                                 ~test-name)
                                ~@(rest test-case))
                               ~expect-val)
                       (= :filter test-type)
                       `(equal (($filter ~(keyword test-name))
                                ~@test-case)
                               ~expect-val)


                       :default
                       `(equal ~test-case ~expect-val)))))
                test-tabular
                (fn [test-type test-name test-body]
                  (for [expr test-body]
                    (if (= :tabular (first expr))
                      (apply concat
                             (expand-tabular expr))
                      expr)))]
            `(deftest ~test-name
               ~@(test-init test-type)
               ~@(test-tabular test-type test-name test-body))
            ))]
    `(do
       (def injector (.. angular (injector [:ng ~(keyword module-name)])))
       (module "tests"
               {:setup
                (fn []
                  (set! (. this -$scope)
                        (.. injector (get :$rootScope) $new)))})
       ~@final-body)))

(defmacro $-
  "Shortcut for $scope.{{symbol}}"
  [sym]
  (symbol (str "$scope." sym)))

(defmacro this->!
  "Remembers the current `this` to be used in `def!`, `defn!`, `!-`"
  []
  (def ^:dynamic *last-this* (ref 0))
  (let [sym (gensym "this")]
    (dosync (ref-set *last-this* sym))
    `(def ~sym this)))

(defmacro def!
  "Shortcut for `(set! that.var-name ...)` where `that` is the scope
  where the last `(this->!)` was called."
  [var-name & [val]]
  `(set! ~(symbol (str (name @*last-this*) "." (name var-name)))
         ~val))

(defmacro defn!
  "Shortcut for `(set! that.fname ...)` where `that` is the scope
  where the last `(this->!)` was called."
  [fname & body]
  `(set! ~(symbol (str (name @*last-this*) "." (name fname)))
         (fn ~@body)))

(defmacro !-
  "Shortcut for `that.var-name` where `that` is the scope
  where the last `(this->!)` was called."
  [sym]
  (symbol (str @*last-this* "." sym)))

(defmacro def$
  "Shortcut for `(set! $scope.var-name ...)`"
  [var-name & [val]]
  `(set! ~(symbol (str "$scope." (name var-name)))
         ~val))

(defmacro def!$
  "Shortcut for `(set! that.$scope.var-name ...)` where `that` is
  the scope where the last `(this->!)` was called."
  [var-name & [val]]
  `(set! ~(symbol (str (name @*last-this*) ".$scope." (name var-name)))
         ~val))

(defmacro defn$
  "Shortcut for `(defn $scope.fname ...)`"
  [fname & body]
  `(set! ~(symbol (str "$scope." (name fname)))
         (fn ~@body)))

(defmacro set-last-app
  "Saves app name (in compiler space) for further usage in `last-app` macro."
  [app-name]
  (def ^:dynamic *last-angular-app* (ref 0))
  (dosync (ref-set *last-angular-app* app-name))
  nil)

(defmacro defapp
  "Creates an Angular app with specified dependencies, associates a var
with the same name to it."
  [app-name app-deps]
  `(do (set-last-app ~app-name)
       (def ~app-name (defmodule (~app-name ~app-deps)))))

(defmacro defsinglemodule
  "Helper macro for `def[directive, controller etc]` macros"
  [module-type body]
  (let [[app-name body]
        ;; if app is specified, body is currently
        ;; `(app-name module-name & more)`
        ;; or `(module-name & more)`

        (cond
         ;; checks the potential app-name
         (and (= :route module-type)
              (or
               (list? (first body))
               (symbol? (first body))))
         [(first body) (rest body)]
         ;; else checks the module-name
         (and (not= :route module-type)
              (symbol? (second body)))
         [(first body) (rest body)]
         ;; else no app is specified then use
         ;; *last-angular-app*
         :else
         [(or @*last-angular-app*
              (throw
               (Exception. "No last Angular app defined!")))
          body])]
    `(defmodule ~app-name
       (~module-type ~@(if (= :route module-type)
                         body
                         [body])))))

(defmacro defdirective
  "Defines a directive for an app"
  [& body]
  `(defsinglemodule :directive ~body))

(defmacro defcontroller
  "Defines a controller for an app"
  [& body]
  `(defsinglemodule :controller ~body))

(defmacro defservice
  "Defines a service for an app"
  [& body]
  `(defsinglemodule :service ~body))

(defmacro deffactory
  "Defines a factory for an app"
  [& body]
  `(defsinglemodule :factory ~body))

(defmacro deffilter
  "Defines a filter for an app. Requires an extra dependency vector."
  [& body]
  `(defsinglemodule :filter ~body))

(defmacro defroute
  "Defines a filter for an app."
  [& body]
  `(defsinglemodule :route ~body))

(defmacro safe-apply
  "Executes a code block that may make changes to scope and call
  $digest (via $apply) when needed."
  [scope & body]
  `(let [func# (fn [] ~@body)]
     (if (or (. ~scope -$$phase) (. ~scope -$root.$$phase))
       (func#)
       (. ~scope ($apply func#)))))

(defmacro $->atom
  "Links a scope attribute to an atom so that everytime the atom
  is changed, the linked scope attribute will get updated to its
  new value (thank to atom's watchers).
  If an updater-fn is specified, the scope attribute will not be updated
  to the atom's value but the value returned by apply updater-fn to
  the atom's new value instead."
  [k an-atom & [updater-fn]]
  (if updater-fn
    `(let [updater-fn ~updater-fn]
       (def$ ~k (updater-fn (deref ~an-atom)))
       (add-watch ~an-atom
                  ~(keyword (gensym (str "$-" k "-")))
                  (fn [_ _ _ new-val]
                    (safe-apply $scope
                                (def$ ~k (updater-fn new-val))))))
    `(do (def$ ~k (deref ~an-atom))
         (add-watch ~an-atom
                    ~(keyword (gensym (str "$-" k "-")))
                    (fn [_ _ _ new-val]
                      (safe-apply $scope
                                  (def$ ~k new-val)))))))
