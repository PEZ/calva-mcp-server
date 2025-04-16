(ns calva-mcp-server.mcp.fxs
  (:require
   [cljs.core.match :refer [match]]
   [calva-mcp-server.mcp.server :as server]))

(defn perform-effect! [dispatch! context effect]
  (match effect
    [:mcp/fx.start-server options]
    (try
      (server/start-server (:mcp/port options))
      (js/console.log "🚀 MCP server started on port" (:mcp/port options))
      (dispatch! context (:ex/on-success options))
      (catch :default e
        (js/console.error "Failed to start MCP server:" e)
        (when-let [on-error (:ex/on-error options)]
          (dispatch! context [(conj (first on-error) (str e))]))))

    [:mcp/fx.stop-server options]
    (try
      (server/stop-server)
      (js/console.log "🛑 MCP server stopped")
      (dispatch! context (:ex/on-success options))
      (catch :default e
        (js/console.error "Failed to stop MCP server:" e)
        (when-let [on-error (:ex/on-error options)]
          (dispatch! context [(conj (first on-error) (str e))]))))

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))