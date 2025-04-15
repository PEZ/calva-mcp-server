(ns calva-mcp-server.vscode.fxs
  (:require
   ["vscode" :as vscode]
   [clojure.core.match :refer [match]]))

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
                       (dispatch! context [(conj then input)])))))))

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))


