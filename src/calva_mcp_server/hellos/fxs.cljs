;; Example commands

(ns calva-mcp-server.hellos.fxs
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.hellos.axs :as hellos]
   [cljs.core.match :refer [match]]
   [promesa.core :as p]))

#_(defn hello-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js hellos/input-box-options)))]
    (vscode/window.showInformationMessage (hellos/greet s))))

(defn hello-command!+ [dispatch! context _!state s]
  (js/console.log "BOOM! hello-command!+" s)
  (dispatch! context [[:hello/ax.log-hello (or s "foo")]]))

(defn new-hello-doc-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js hellos/input-box-options)))
          document (vscode/workspace.openTextDocument #js {:content (hellos/greet s)})]
    (vscode/window.showTextDocument document)))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    :else
    (js/console.error "Unknown effect:" (pr-str effect))))