(ns tests.smoke-test
  (:require ["vscode" :as vscode]
            [cljs.test :refer [deftest testing is]]
            [promesa.core :as p]
            [e2e.macros :refer [deftest-async]]))

(deftest foo
  (testing "We can test sync things"
    (is (= :foo
           :foo)
        "A is A")
    (is (= 1
           (count vscode/workspace.workspaceFolders))
        "There can only be one (workspaceFolder)")))

(deftest-async extension-activation
  (p/let [extension (vscode/extensions.getExtension "betterthantomorrow.calva-backseat-driver")
          api (.activate extension)]
    (is (not= nil
              (.-v1 api))
        "The extension activates (which is an async operation)")))
