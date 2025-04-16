(ns tests.hello-doc-test
  (:require ["vscode" :as vscode]
            [cljs.test :refer [testing is]]
            [promesa.core :as p]
            [e2e.macros :refer [deftest-async]]))

(deftest-async new-hello-doc
  (testing "Context of the test assertions"
    (p/let [hello-text "Multiverse"
            editor (vscode/commands.executeCommand "calva-mcp-server.newHelloDocument" hello-text)
            doc (.-document editor)
            doc-text (.getText doc)
            _ (vscode/commands.executeCommand "workbench.action.revertAndCloseActiveEditor" false)]
      (is (= (str "Hello, " hello-text "!")
             doc-text)
          "Hello doc created with the expected text"))))
