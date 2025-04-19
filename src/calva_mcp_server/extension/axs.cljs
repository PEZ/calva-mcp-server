(ns calva-mcp-server.extension.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:extension/ax.set-when-context k v]
    {:ex/db (assoc-in state [:extension/when-contexts k] v)
     :ex/fxs [[:vscode/fx.set-context k v]]}

    [:extension/ax.set-min-log-level level]
    {:ex/db (assoc state :app/min-log-level level)}

    [:extension/ax.log {:keys [level message]}]
    (let [min-level (get state :app/min-log-level :debug)
          levels {:error 0, :warn 1, :info 2, :debug 3}]
      (when (<= (get levels level) (get levels min-level))
        {:ex/fxs [[:extension/fx.log {:level level :message message}]]}))

    :else nil))

