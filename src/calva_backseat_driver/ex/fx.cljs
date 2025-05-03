(ns calva-backseat-driver.ex.fx
  (:require
   [calva-backseat-driver.ex.ax :as ax]
   [calva-backseat-driver.integrations.calva.fxs :as calva-fxs]
   [calva-backseat-driver.integrations.node.fxs :as node-fxs]
   [calva-backseat-driver.integrations.vscode.fxs :as vscode-fxs]
   [calva-backseat-driver.mcp.fxs :as mcp-fxs]
   [calva-backseat-driver.app.fxs :as app-fxs]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [dispatch! context [effect-kw :as effect]]
  (let [enriched-effect (-> effect
                            (ax/enrich-from-context context))]
    (match (namespace effect-kw)
      "node"   (node-fxs/perform-effect! dispatch! context enriched-effect)
      "vscode" (vscode-fxs/perform-effect! dispatch! context enriched-effect)
      "mcp"    (mcp-fxs/perform-effect! dispatch! context enriched-effect)
      "app"    (app-fxs/perform-effect! dispatch! context enriched-effect)
      "calva"  (calva-fxs/perform-effect! dispatch! context enriched-effect)
      :else (js/console.warn "Unknown effect namespace:" (pr-str enriched-effect)))))