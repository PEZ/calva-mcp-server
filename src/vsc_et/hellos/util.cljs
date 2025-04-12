;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns vsc-et.hellos.util)

(defn greet [s]
  (str "Hello, " s "!"))

(def input-box-options {:title "Hello Input"
                        :placeHolder "What should we say hello to today?"})