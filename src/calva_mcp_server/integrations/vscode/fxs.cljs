(ns calva-mcp-server.integrations.vscode.fxs
  (:require
   ["vscode" :as vscode]
   [clojure.core.match :refer [match]]
   [calva-mcp-server.ex.ax :as ax]))

(defn perform-effect! [dispatch! context effect]
  (match effect
    [:vscode/fx.show-information-message & args]
    (apply vscode/window.showInformationMessage args)

    [:vscode/fx.show-input-box options]
    (let [{:ex/keys [then]} options]
      (if-not then
        (vscode/window.showInputBox options)
        (-> (vscode/window.showInputBox (clj->js options))
            (.then (fn [input]
                     (when input
                       (dispatch! context (ax/enrich-with-args then input))))))))

    [:vscode/fx.open-text-document options]
    (let [{:keys [ex/then app/content]} options]
      (if-not then
        (vscode/window.showInputBox options)
        (-> (vscode/workspace.openTextDocument #js {:content content})
            (.then (fn [document]
                     (when document
                       (dispatch! context (ax/enrich-with-args then document))))))))

    [:vscode/fx.show-text-document document]
    (vscode/window.showTextDocument document)

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))
