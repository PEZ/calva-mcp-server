(ns calva-mcp-server.mcp.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:mcp/ax.start-server]
    {:ex/db (assoc state :mcp/server-running? true)
     :ex/fxs [[:mcp/fx.start-server]
              [:vscode/fx.show-information-message "MCP server started on port 3000"]]}

    [:mcp/ax.stop-server]
    {:ex/db (assoc state :mcp/server-running? false)
     :ex/fxs [[:mcp/fx.stop-server]
              [:vscode/fx.show-information-message "MCP server stopped"]]}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))