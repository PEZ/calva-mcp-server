;; Example commands

(ns calva-mcp-server.hello.fxs
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.hello.axs :as hellos]
   [cljs.core.match :refer [match]]
   [promesa.core :as p]))

(defn hello-command!+ [dispatch! context _!state s]
  (js/console.log "BOOM! hello-command!+" s)
  (dispatch! context [[:hello/ax.command.hello {:greetee s}]]))

(defn new-hello-doc-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js hellos/input-box-options)))
          document (vscode/workspace.openTextDocument #js {:content (hellos/greet s)})]
    (vscode/window.showTextDocument document)))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    :else
    (js/console.error "Unknown effect:" (pr-str effect))))