;; Example commands

(ns calva-mcp-server.hello.fxs
  (:require
   [cljs.core.match :refer [match]]))

#_(defn new-hello-doc-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js hello/input-box-options)))
          document (vscode/workspace.openTextDocument #js {:content (hello/greet s)})]
    (vscode/window.showTextDocument document)))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    :else
    (js/console.error "Unknown effect:" (pr-str effect))))