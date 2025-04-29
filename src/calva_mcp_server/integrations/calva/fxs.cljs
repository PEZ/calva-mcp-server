(ns calva-mcp-server.integrations.calva.fxs
  (:require
   [clojure.core.match :refer [match]]
   [calva-mcp-server.integrations.calva.api :as calva]))

(defn perform-effect! [dispatch! context effect]
  (match effect
    [:calva/fx.subscribe-to-output on-output]
    (let [unsubscribe-fn (calva/subscribe-to-output {:ex/dispatch! #(dispatch! context [%])
                                                   :calva/on-output on-output})]
      (dispatch! [[:db/ax.update-in :calva/output-unsubscribe-fn unsubscribe-fn]]))

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))
