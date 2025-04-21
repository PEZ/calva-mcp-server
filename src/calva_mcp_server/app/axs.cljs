(ns calva-mcp-server.app.axs
  (:require
   [cljs.core.match :refer [match]]))


(defn handle-action [state _context action]
  (match action
    [:app/ax.activate initial-state]
    {:ex/dxs [[:app/ax.init initial-state]
              [:app/ax.register-command "calva-mcp-server.newHelloDocument" [[:hello/ax.command.hello-doc
                                                                              {:greetee :ex/action-args%1}]]]
              [:app/ax.register-command "calva-mcp-server.hello" [[:hello/ax.command.hello
                                                                   {:greetee :ex/action-args%1}]]]
              [:app/ax.register-command "calva-mcp-server.startServer" [[:mcp/ax.start-server]]]
              [:app/ax.register-command "calva-mcp-server.stopServer" [[:mcp/ax.stop-server]]]
              [:app/ax.register-command "calva-mcp-server.openServerLog" [[:mcp/ax.open-server-log]]]
              [:app/ax.set-when-context :calva-mcp-extension/activated? true]]}

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

    [:app/ax.clear-disposables]
    {:ex/db (assoc state :extension/disposables [])
     :ex/fxs [[:app/fx.clear-disposables (:extension/disposables state)]]}

    [:app/ax.cleanup]
    {:ex/dxs [[:app/ax.set-when-context :calva-mcp-extension/activated? false]
              [:app/ax.clear-disposables]]}

    [:app/ax.deactivate]
    {:ex/dxs [[:mcp/ax.stop-server]
              [:app/ax.cleanup]]}

    :else nil))

