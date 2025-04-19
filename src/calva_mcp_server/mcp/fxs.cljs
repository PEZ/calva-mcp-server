(ns calva-mcp-server.mcp.fxs
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.mcp.server :as server]
   [cljs.core.match :refer [match]]
   [promesa.core :as p]))

(defn perform-effect! [dispatch! context effect]
  (match effect
    [:mcp/fx.start-server options]
    (let [{:ex/keys [on-success on-error]} options]
      (-> (server/start-server!+ options)
          (p/then (fn [{:server/keys [port] :as server-info}]
                    (js/console.log "🚀 MCP server started on port" port)
                    (dispatch! context (ax/enrich-with-args on-success server-info))))
          (p/catch

           (fn [e]
             (js/console.error "Failed to start MCP server:" e)
             (dispatch! context (ax/enrich-with-args on-error e))))))

    [:mcp/fx.stop-server options]
    (let [{:ex/keys [on-success on-error]} options]
      (-> (p/catch
           (server/stop-server!+ options)
           (fn [e]
             (js/console.error "Failed to stop MCP server:" (.-message e))
             (dispatch! context (ax/enrich-with-args on-error (.-message e)))))
          (p/then (fn [_]
                    (js/console.log "🛑 MCP server stopped")
                    (dispatch! context on-success)))))

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))