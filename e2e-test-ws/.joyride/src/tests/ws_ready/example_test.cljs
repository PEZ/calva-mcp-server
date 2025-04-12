(ns tests.ws-ready.example-test
  (:require [cljs.test :refer [deftest testing is]]
            ["vscode" :as vscode]
            workspace-activate))

; No tests starts before the workspace is activated
(deftest ws-activated
  (testing "The workspace is indeed activated"
    (is (= 42 workspace-activate/question))
    (is (= (first vscode/workspace.workspaceFolders) workspace-activate/ws-root))))