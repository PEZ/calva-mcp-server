(ns calva-mcp-server.app.axs
  (:require
   [cljs.core.match :refer [match]]))


(defn handle-action [state _context action]
  (match action
    [:app/ax.init initial-state]
    {:ex/db (merge state initial-state)
     :ex/fxs [[:app/fx.init-logging initial-state]]}

    [:app/ax.set-when-context k v]
    {:ex/db (assoc-in state [:extension/when-contexts k] v)
     :ex/fxs [[:vscode/fx.set-context k v]]}

    [:app/ax.set-min-log-level level]
    {:ex/db (assoc state :app/min-log-level level)}

    [:app/ax.log level & messages]
    {:ex/fxs [[:app/fx.log
               (select-keys state [:app/min-log-level :app/log-file-uri])
               level
               messages]]}

    [:app/ax.register-command command-id actions]
    {:ex/fxs [[:app/fx.register-command command-id actions]]}

    :else nil))

