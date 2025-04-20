(ns calva-mcp-server.extension.axs
  (:require
   [cljs.core.match :refer [match]]))


(defn handle-action [state _context action]
  (match action
    [:extension/ax.init initial-state]
    {:ex/db (merge state initial-state)
     :ex/fxs [[:extension/fx.init-logging initial-state]]}

    [:extension/ax.set-when-context k v]
    {:ex/db (assoc-in state [:extension/when-contexts k] v)
     :ex/fxs [[:vscode/fx.set-context k v]]}

    [:extension/ax.set-min-log-level level]
    {:ex/db (assoc state :app/min-log-level level)}

    [:extension/ax.log level & messages]
    {:ex/fxs [[:extension/fx.log
               (select-keys state [:app/min-log-level :app/log-file-uri])
               level
               messages]]}

    :else nil))

