(ns calva-mcp-server.ex.fx
  (:require [clojure.core.match :refer [match]]
            [calva-mcp-server.node.fxs :as node-fxs]
            [calva-mcp-server.hello.fxs :as hello-fxs]
            [calva-mcp-server.vscode.fxs :as vscode-fxs]))

(defn perform-effect! [dispatch! context [effect-kw :as effect]]
  (match (namespace effect-kw)
    "node" (node-fxs/perform-effect! dispatch! context effect)
    "hello" (hello-fxs/perform-effect! dispatch! context effect)
    "vscode" (vscode-fxs/perform-effect! dispatch! context effect)
    :else (js/console.warn "Unknown effect namespace:" (pr-str effect))))