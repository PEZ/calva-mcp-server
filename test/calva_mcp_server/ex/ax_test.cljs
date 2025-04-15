(ns calva-mcp-server.ex.ax-test
  (:require [cljs.test :refer [deftest testing is]]
            [calva-mcp-server.ex.ax :as ax]))

(deftest enrich-action
  (testing "Enriching action from context"
    (let [state {}
          ctx {:name "World"}
          action [:hello/ax.say-hello :ctx/name]
          result (ax/handle-action state ctx action)]
      (is (= "World"
             (:hello/last-greetee (:ex/db result))))
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]]
             (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]]
             (:ex/dxs result)))))

  (testing "Enriching action from state"
    (let [state {:user-name "Clojurian"}
          ctx {}
          action [:hello/ax.say-hello [:db/get :user-name]]
          result (ax/handle-action state ctx action)]
      (is (= "Clojurian" (:hello/last-greetee (:ex/db result))))
      (is (= [[:vscode/fx.show-information-message "Hello, Clojurian!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]] (:ex/dxs result))))))

(deftest handle-actions
  (is (= {:ex/db {:hello/last-greetee "Test"},
          :ex/dxs [[:hello/ax.greeting-sent]],
          :ex/fxs [[:vscode/fx.show-information-message "Hello, Test!"]]}
         (ax/handle-actions {} {:name "World"} [[:hello/ax.say-hello "Test"]]))
      "processes an action returning db dxs, and fxs")

  (is (= {:ex/db {:hello/last-greetee "Clojure"},
          :ex/dxs [[:hello/ax.greeting-sent]],
          :ex/fxs [[:vscode/fx.show-information-message "Hello, Clojure!"]]}
         (ax/handle-actions {} {:name "Clojure"} [[:hello/ax.say-hello :ctx/name]]))
      "enriches action from context")

  (testing "Handling multiple actions"
    (let [state {}
          ctx {:name "World"}
          actions [[:hello/ax.say-hello :ctx/name]
                   [:hello/ax.say-hello "Calva"]]
          {:ex/keys [db fxs dxs]} (ax/handle-actions state ctx actions)]
      (is (= "Calva"
             (:hello/last-greetee db))
          "Last action determines final state")
      (is (= [[:vscode/fx.show-information-message "Hello, World!"]
              [:vscode/fx.show-information-message "Hello, Calva!"]]
             fxs)
          "Effects are accumulated")
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]]
             dxs)
          "Dispatches are accumulated"))))
