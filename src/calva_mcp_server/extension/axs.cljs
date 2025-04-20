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

    [:extension/ax.log level & messages]
    (let [min-level (:app/min-log-level state :debug)
          levels {:error 0, :warn 1, :info 2, :debug 3}]
      (apply println "BOOM! :extension/ax.log" level messages)
      (when (<= (levels level) (levels min-level))
        {:ex/fxs [(conj [:extension/fx.log {:app/log-uri :context/logUri} level] messages)]}))

    :else nil))

