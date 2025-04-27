(ns calva-mcp-server.ex.fx
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.integrations.node.fxs :as node-fxs]
   [calva-mcp-server.integrations.vscode.fxs :as vscode-fxs]
   [calva-mcp-server.mcp.fxs :as mcp-fxs]
   [calva-mcp-server.app.fxs :as app-fxs]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [dispatch! context [effect-kw :as effect]]
  (let [enriched-effect (-> effect
                            (ax/enrich-from-context context))]
    (match (namespace effect-kw)
      "node"   (node-fxs/perform-effect! dispatch! context enriched-effect)
      "vscode" (vscode-fxs/perform-effect! dispatch! context enriched-effect)
      "mcp"    (mcp-fxs/perform-effect! dispatch! context enriched-effect)
      "app"    (app-fxs/perform-effect! dispatch! context enriched-effect)
      :else (js/console.warn "Unknown effect namespace:" (pr-str enriched-effect)))))