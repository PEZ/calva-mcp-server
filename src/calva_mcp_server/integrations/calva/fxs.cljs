(ns calva-mcp-server.integrations.calva.fxs
  (:require
   [clojure.core.match :refer [match]]
   [calva-mcp-server.integrations.calva.api :as calva]))

(defn perform-effect! [dispatch! ^js context effect]
  (match effect
    [:calva/fx.when-activated actions]
    (calva/when-calva-activated {:ex/dispatch! (partial dispatch! context)
                                 :ex/then actions})

    [:calva/fx.subscribe-to-output on-output]
    (let [disposable (calva/subscribe-to-output {:ex/dispatch! #(dispatch! context [%])
                                                 :calva/on-output on-output})]
      (.push (.-subscriptions context) disposable))

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))
