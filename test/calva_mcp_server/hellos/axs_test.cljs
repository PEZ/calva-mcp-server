;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns calva-mcp-server.hellos.axs-test
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.hellos.axs :as sut]
   [clojure.test :refer [deftest is testing]]))

(deftest greet
  (testing "greet"
    (is (.startsWith (sut/greet "World") "Hello, "))
    (is (.endsWith (sut/greet "World") "!"))
    (is (= "World" (subs (sut/greet "World") 7 12)))))

(deftest say-hello-action
  (testing "Stateless event handler processes actions"
    (is (= {:ex/db {:hello/last-greetee "Test"},
            :ex/dxs [[:hello/ax.greeting-sent]],
            :ex/fxs [[:vscode/fx.show-information-message "Hello, Test!"]]}
           (ax/handle-action {} {:name "World"} [:hello/ax.say-hello "Test"]))))

  (testing "Stateless event handler processes context values"
    (is (= {:ex/db {:hello/last-greetee "Clojure"},
            :ex/dxs [[:hello/ax.greeting-sent]],
            :ex/fxs [[:vscode/fx.show-information-message "Hello, Clojure!"]]}
           (ax/handle-action {} {:name "Clojure"} [:hello/ax.say-hello :ctx/name])))))

(deftest hello-action
  (testing "Handling multiple actions"
    (let [state {}
          ctx {:name "World"}
          actions [[:hello/ax.say-hello :ctx/name]
                   [:hello/ax.say-hello "Calva"]]
          result (ax/handle-actions state ctx actions)]
      (is (= "Calva"
             (:hello/last-greetee (:ex/db result))))
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]
              [:vscode/fx.show-information-message "Hello, Calva!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]] (:ex/dxs result))))))