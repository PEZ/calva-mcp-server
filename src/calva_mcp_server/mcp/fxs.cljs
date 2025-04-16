(ns calva-mcp-server.mcp.fxs
  (:require
   [cljs.core.match :refer [match]]
   [calva-mcp-server.mcp.server :as server]))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    [:mcp/fx.start-server]
    (server/start-server)

    [:mcp/fx.stop-server]
    (server/stop-server)

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))