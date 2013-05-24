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
