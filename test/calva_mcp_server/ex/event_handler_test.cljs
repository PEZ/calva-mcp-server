(ns calva-mcp-server.ex.event-handler-test
  (:require [cljs.test :refer [deftest testing is]]
            [calva-mcp-server.ex.event-handler :as event-handler]
            [calva-mcp-server.ex.ax :as ax]))

(deftest event-handler-test
  (testing "Stateless event handler processes actions"
    (let [ctx {:name "World"}
          actions [[:hello/ax.say-hello "Test"]]
          result (event-handler/event-handler ctx actions)]
      ;; Test that we got the expected state
      (is (= "Hello, Test!" (:hello/last-greeting result)))))

  (testing "Stateless event handler processes context values"
    (let [ctx {:name "Clojure"}
          actions [[:hello/ax.say-hello :ctx/name]]
          result (event-handler/event-handler ctx actions)]
      ;; Test that we got the expected state with value from context
      (is (= "Hello, Clojure!" (:hello/last-greeting result))))))

(deftest create-event-handler-test
  (testing "Stateful event handler maintains state between calls"
    (let [handler (event-handler/create-event-handler {})
          ctx {:name "World"}]
      ;; First action
      (let [result (handler ctx [[:hello/ax.say-hello :ctx/name]])]
        (is (= "Hello, World!" (:hello/last-greeting result)))
        (is (:hello/greeting-sent? result)))

      ;; Second action should work with the updated state
      (let [result (handler ctx [[:hello/ax.say-hello "Calva"]])]
        (is (= "Hello, Calva!" (:hello/last-greeting result)))
        (is (:hello/greeting-sent? result))))))