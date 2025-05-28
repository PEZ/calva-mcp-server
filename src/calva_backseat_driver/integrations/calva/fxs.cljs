(ns calva-backseat-driver.integrations.calva.fxs
  (:require
   [clojure.core.match :refer [match]]
   [calva-backseat-driver.integrations.calva.api :as calva]
   [calva-backseat-driver.integrations.calva.features :as calva-features]))

(defn perform-effect! [dispatch! ^js context effect]
  (match effect
    [:calva/fx.when-activated actions]
    (calva/when-calva-activated {:ex/dispatch! (partial dispatch! context)
                                 :ex/then actions})

    [:calva/fx.subscribe-to-output on-output]
    (let [disposable (calva-features/subscribe-to-output {:ex/dispatch! #(dispatch! context [%])
                                                          :calva/on-output on-output})]
      (.push (.-subscriptions context) disposable))

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))
