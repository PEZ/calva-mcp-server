(ns calva-mcp-server.vscode.fxs
  (:require
   ["vscode" :as vscode]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    [:vscode/fx.show-information-message & args]
    (apply vscode/window.showInformationMessage args)

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))