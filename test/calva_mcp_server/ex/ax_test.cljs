(ns calva-mcp-server.ex.ax-test
  (:require [cljs.test :refer [deftest testing is]]
            [calva-mcp-server.ex.ax :as ax]))

(deftest enrich-action-test
  (testing "Enriching action from context"
    (let [state {}
          ctx {:name "World"}
          action [:hello/ax.say-hello :ctx/name]
          result (ax/handle-action state ctx action)]
      (is (= "Hello, World!" (:hello/last-greeting (:ex/db result))))
      (is (= [[:hello/fx.log-greeting "Hello, World!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]] (:ex/dxs result)))))

  (testing "Enriching action from state"
    (let [state {:user-name "Clojurian"}
          ctx {}
          action [:hello/ax.say-hello [:db/get :user-name]]
          result (ax/handle-action state ctx action)]
      (is (= "Hello, Clojurian!" (:hello/last-greeting (:ex/db result))))
      (is (= [[:hello/fx.log-greeting "Hello, Clojurian!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]] (:ex/dxs result))))))

(deftest handle-actions-test
  (testing "Handling multiple actions"
    (let [state {}
          ctx {:name "World"}
          actions [[:hello/ax.say-hello :ctx/name]
                   [:hello/ax.say-hello "Calva"]]
          result (ax/handle-actions state ctx actions)]
      (is (= "Hello, Calva!" (:hello/last-greeting (:ex/db result))))
      (is (= [[:hello/fx.log-greeting "Hello, World!"]
              [:hello/fx.log-greeting "Hello, Calva!"]] (:ex/fxs result)))
      (is (= [[:hello/ax.greeting-sent]
              [:hello/ax.greeting-sent]] (:ex/dxs result))))))