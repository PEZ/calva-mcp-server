;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns vsc-et.hellos.util-test
  (:require [vsc-et.hellos.util :as sut]
            [clojure.test :refer [deftest is testing]]))

(deftest greet
  (testing "greet"
    (is (.startsWith (sut/greet "World") "Hello, "))
    (is (.endsWith (sut/greet "World") "!"))
    (is (= "World" (subs (sut/greet "World") 7 12)))))