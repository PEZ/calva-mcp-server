(ns calva-mcp-server.vscode.axs
  (:require [clojure.core.match :refer [match]]))

(defn handle-action [_state _context action]
  (match action
    [:vscode/ax.show-information-message & args]
    {:ex/fxs [(into [:vscode/fx.show-information-message] args)]}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" action]]}))