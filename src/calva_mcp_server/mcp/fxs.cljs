(ns calva-mcp-server.mcp.fxs
  (:require
   [cljs.core.match :refer [match]]
   [promesa.core :as p]
   [calva-mcp-server.mcp.server :as server]))

(defn perform-effect! [dispatch! context effect]
  (match effect
    [:mcp/fx.start-server options]
    (-> (p/catch
         (server/start-server (:mcp/port options))
         (fn [e]
           (js/console.error "Failed to start MCP server:" e)
           (when-let [on-error (:ex/on-error options)]
             (dispatch! context [(conj (first on-error) (str e))]))
           (throw e)))
        (p/then (fn [_]
                  (js/console.log "🚀 MCP server started on port" (:mcp/port options))
                  (when-let [on-success (:ex/on-success options)]
                    (dispatch! context on-success)))))

    [:mcp/fx.stop-server options]
    (-> (p/catch
         (server/stop-server)
         (fn [e]
           (js/console.error "Failed to stop MCP server:" e)
           (when-let [on-error (:ex/on-error options)]
             (dispatch! context [(conj (first on-error) (str e))]))
           (throw e)))
        (p/then (fn [_]
                  (js/console.log "🛑 MCP server stopped")
                  (when-let [on-success (:ex/on-success options)]
                    (dispatch! context on-success)))))

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))