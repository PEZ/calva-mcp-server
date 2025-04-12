;; Example commands

(ns vsc-et.hellos
  (:require ["vscode" :as vscode]
            [promesa.core :as p]
            [vsc-et.hellos.util :as util]))

(defn hello-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js util/input-box-options)))]
    (vscode/window.showInformationMessage (util/greet s))))

(defn new-hello-doc-command!+ [_!state s]
  (p/let [s (or s (vscode/window.showInputBox (clj->js util/input-box-options)))
          document (vscode/workspace.openTextDocument #js {:content (util/greet s)})]
    (vscode/window.showTextDocument document)))
