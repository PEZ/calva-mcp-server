(ns calva-backseat-driver.ex.ax-test
  (:require [cljs.test :refer [deftest testing is]]
            [calva-backseat-driver.ex.ax :as ax]))

(deftest enrich-action
  (testing "Enriching action from context"
    (let [result (ax/handle-action {}
                                   #js {:name "World"}
                                   [:ex-test/ax.log-message :context/name])]
      (is (= "World"
             (:ex-test/last-message (:ex/db result)))
          "new state has name from context")
      (is (= [[:node/fx.log "ex-test" "World"]]
             (:ex/fxs result))
          "fxs uses name from context")))

  (testing "Enriching action from state"
    (let [result (ax/handle-action {:user-name "Clojurian"}
                                   {}
                                   [:ex-test/ax.log-message [:db/get :user-name]])]
      (is (= "Clojurian"
             (:ex-test/last-message (:ex/db result)))
          "new state has user-name from state")
      (is (= [[:node/fx.log "ex-test" "Clojurian"]]
             (:ex/fxs result))
          "fxs uses user name from state"))))

(deftest handle-actions
  (is (= {:ex/db {:ex-test/ax.message-logged false
                  :ex-test/last-message "Test"},
          :ex/dxs [[:ex-test/ax.message-logged]],
          :ex/fxs [[:node/fx.log "ex-test" "Test"]]}
         (ax/handle-actions {} {:name "World"} [[:ex-test/ax.log-message "Test"]]))
      "processes an action returning db dxs, and fxs")

  (testing "Handling multiple actions"
    (let [{:ex/keys [db fxs dxs]} (ax/handle-actions {}
                                                     #js {:name "World"}
                                                     [[:ex-test/ax.log-message :context/name]
                                                      [:ex-test/ax.log-message "Calva"]])]
      (is (= "Calva"
             (:ex-test/last-message db))
          "Last action determines final state")
      (is (= [[:node/fx.log "ex-test" "World"]
              [:node/fx.log "ex-test" "Calva"]]
             fxs)
          "Effects are accumulated")
      (is (= [[:ex-test/ax.message-logged]
              [:ex-test/ax.message-logged]]
             dxs)
          "Dispatches are accumulated"))))
