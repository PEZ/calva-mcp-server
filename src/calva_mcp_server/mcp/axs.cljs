(ns calva-mcp-server.mcp.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:mcp/ax.start-server port]
    {:ex/db (assoc state :mcp/server-running? true :mcp/server-port port)
     :ex/fxs [[:mcp/fx.start-server {:mcp/port port
                                     :ex/on-success [[:vscode/ax.show-information-message (str "MCP server started on port " port)]
                                                     [:mcp/ax.update-context true]]
                                     :ex/on-error [[:mcp/ax.server-error]]}]]}

    [:mcp/ax.stop-server]
    {:ex/db (assoc state :mcp/server-running? false)
     :ex/fxs [[:mcp/fx.stop-server {:ex/on-success [[:vscode/ax.show-information-message "MCP server stopped"]
                                                    [:mcp/ax.update-context false]]
                                    :ex/on-error [[:mcp/ax.server-error]]}]]}

    [:mcp/ax.update-context running?]
    {:ex/fxs [[:vscode/fx.set-context "calva-mcp:server-started" running?]]}

    [:mcp/ax.server-error err]
    {:ex/fxs [[:vscode/fx.show-error-message (str "MCP server error: " err)]]}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))