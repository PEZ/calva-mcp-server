(ns calva-mcp-server.node.axs
  (:require [clojure.core.match :refer [match]]))

(defn handle-action [state ctx action]
  (match action
    [:node/ax.log & args]
    {:ex/fxs [(into [:node/fx.log] args)]}

    :else
    {:ex/fxs [[:node/fx.log-error-greeting "Unknown action:" action]]}))