;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns calva-mcp-server.hello.axs-test
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [clojure.test :refer [deftest is testing]]))

(deftest say-hello-action
  (let [result (ax/handle-actions {:name "World"}
                                  nil
                                  [[:hello/ax.say-hello "Clojurian"]])]
    (is (= "Clojurian"
           (:hello/last-greetee (:ex/db result)))
        "Say greetee is saved in new state")
    (is (= [[:vscode/fx.show-information-message "Hello, Clojurian!"]] (:ex/fxs result))
        "The greetee is greeted")
    (is (= [[:hello/ax.greeting-sent]] (:ex/dxs result))
        "Action about greet updated is dispatched")))

(deftest hello-action
  (testing "Handling multiple actions"
    (let [result (ax/handle-actions {:name "World"}
                                    nil
                                    [[:hello/ax.say-hello [:db/get :name]]
                                     [:hello/ax.say-hello "Calva"]])]
      (is (= "Calva"
             (:hello/last-greetee (:ex/db result)))
          "Last say saved in new state")
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]
              [:vscode/fx.show-information-message "Hello, Calva!"]] (:ex/fxs result))
          "Both says are shown")
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]] (:ex/dxs result))
          "Both dispatches are present"))))

(deftest command-hello-doc-action
  (let [result (ax/handle-actions {:name "World"}
                                  nil
                                  [[:hello/ax.command.hello-doc {:greetee "DocClojurian"}]])]
    (is (= "DocClojurian"
           (:hello/last-greetee (:ex/db result)))
        "Doc greetee is saved in new state")
    (is (= [[:vscode/fx.open-text-document {:app/content "Hello, DocClojurian!"
                                            :ex/then [[:vscode/ax.show-text-document :ex/action-args%1]]}]]
           (:ex/fxs result))
        "The greetee doc is opened with correct content and then show-text-document is queued")
    (is (= [[:hello/ax.greeting-sent]]
           (:ex/dxs result))
        "Action about greet updated is dispatched for doc action")))