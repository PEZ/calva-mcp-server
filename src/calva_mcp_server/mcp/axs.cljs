(ns calva-mcp-server.mcp.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:mcp/ax.start-server]
    {:ex/db (assoc state :app/server-starting? true)
     :ex/dxs [[:extension/ax.set-when-context "calva-mcp:server-starting" true]]
     :ex/fxs [[:mcp/fx.start-server {:app/log-uri :context/logUri
                                     :ex/on-success [[:mcp/ax.server-started :ex/action-args]]
                                     :ex/on-error [[:mcp/ax.server-error :ex/action-args]]}]]}

    [:mcp/ax.server-started server-info]
    {:ex/db (assoc state
                   :app/server-info server-info
                   :app/server-starting? false)
     :ex/dxs [[:extension/ax.set-when-context "calva-mcp:server-starting" false]
              [:extension/ax.set-when-context "calva-mcp:server-started" true]]
     :ex/fxs [[:vscode/fx.show-information-message (str "MCP server started on port" (:server/port server-info))]]}

    [:mcp/ax.stop-server]
    {:ex/db (assoc state :app/server-stopping? true)
     :ex/dxs [[:extension/ax.set-when-context "calva-mcp:server-stopping" true]]
     :ex/fxs [[:mcp/fx.stop-server (merge {:app/log-uri :context/logUri
                                           :ex/on-success [[:mcp/ax.server-stopped]]
                                           :ex/on-error [[:mcp/ax.server-error :ex/action-args]]}
                                          (:app/server-info state))]]}

    [:mcp/ax.server-stopped]
    {:ex/db (dissoc state
                    :app/server-info
                    :app/server-stopping?)
     :ex/dxs [[:extension/ax.set-when-context "calva-mcp:server-stopping" false]
              [:extension/ax.set-when-context "calva-mcp:server-started" false]]
     :ex/fxs [[:vscode/fx.show-information-message "MCP server stopped"]]}

    [:mcp/ax.open-server-log]
    {:ex/fxs [[:vscode/fx.workspace.open-text-document
               {:open/uri (get-in state [:app/server-info :server/log-uri])
                :ex/then [[:vscode/ax.show-text-document :ex/action-args]]}]]}

    [:mcp/ax.server-error err]
    (do (js/console.error err)
      {:ex/fxs [[:vscode/fx.show-error-message (str "MCP server error: " err)]]})

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))