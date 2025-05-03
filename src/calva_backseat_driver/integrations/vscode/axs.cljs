(ns calva-backseat-driver.integrations.vscode.axs
  (:require [clojure.core.match :refer [match]]))

(defn handle-action [_state _context action]
  (match action
    [:vscode/ax.show-information-message & args]
    {:ex/fxs [(into [:vscode/fx.show-information-message] args)]}

    [:vscode/ax.show-text-document document]
    {:ex/fxs [[:vscode/fx.show-text-document document]]}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))

