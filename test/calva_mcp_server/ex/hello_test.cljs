(ns calva-mcp-server.ex.hello-test
  (:require [cljs.test :refer [deftest testing is]]
            [calva-mcp-server.ex.ax :as ax]))

(deftest hello-action
  (testing "Stateless event handler processes actions"
    (is (= {:ex/db {:hello/last-greetee "Hello, Test!"},
            :ex/dxs [[:hello/ax.greeting-sent]],
            :ex/fxs [[:vscode/fx.show-information-message "Hello, Test!"]]}
           (ax/handle-action {} {:name "World"} [:hello/ax.say-hello "Test"]))))

  (testing "Stateless event handler processes context values"
    (is (= {:ex/db {:hello/last-greetee "Hello, Clojure!"},
            :ex/dxs [[:hello/ax.greeting-sent]],
            :ex/fxs [[:vscode/fx.show-information-message "Hello, Clojure!"]]}
           (ax/handle-action {} {:name "Clojure"} [:hello/ax.say-hello :ctx/name])))))

(deftest handle-actions-test
  (testing "Handling multiple actions"
    (let [state {}
          ctx {:name "World"}
          actions [[:hello/ax.say-hello :ctx/name]
                   [:hello/ax.say-hello "Calva"]]
          result (ax/handle-actions state ctx actions)]
      (is (= "Hello, Calva!" (:hello/last-greetee (:ex/db result))))
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]
              [:vscode/fx.show-information-message "Hello, Calva!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]] (:ex/dxs result))))))