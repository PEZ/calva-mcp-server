(ns calva-mcp-server.extension.life-cycle-helpers
  (:require
   [calva-mcp-server.ex.ex :as ex]))


;;;;; Extension lifecycle helper functions
;; These also assist with managing `vscode/Disposable`s in a hot-reloadable way.

(defn- clear-disposables! [!state]
  (doseq [^js disposable (:extension/disposables @!state)]
    (.dispose disposable))
  (swap! !state assoc :extension/disposables []))

(defn cleanup! [!state]
  (when (:extension/context @!state)
    (ex/dispatch! (:extension/context @!state) [[:app/ax.set-when-context :calva-mcp-server/active? false]]))
  (clear-disposables! !state))

