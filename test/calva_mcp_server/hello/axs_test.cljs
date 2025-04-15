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
   [calva-mcp-server.hello.axs :as sut]
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
           (ax/handle-action {} {:name "World"} [:hello/ax.say-hello "Test"])))))

(deftest hello-action
  (testing "Handling multiple actions"
    (let [state {:name "World"}
          actions [[:hello/ax.say-hello [:db/get :name]]
                   [:hello/ax.say-hello "Calva"]]
          result (ax/handle-actions state nil actions)]
      (is (= "Calva"
             (:hello/last-greetee (:ex/db result))))
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]
              [:vscode/fx.show-information-message "Hello, Calva!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]] (:ex/dxs result))))))