(ns calva-mcp-server.extension.life-cycle-helpers
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.ex.ex :as ex]
   [calva-mcp-server.extension.when-contexts :as when-contexts]))


;;;;; Extension lifecycle helper functions
;; These also assist with managing `vscode/Disposable`s in a hot-reloadable way.

(defn push-disposable! [!state ^js disposable]
  (swap! !state update :extension/disposables conj disposable)
  (.push (.-subscriptions ^js (:extension/context @!state)) disposable))

(defn- clear-disposables! [!state]
  (doseq [^js disposable (:extension/disposables @!state)]
    (.dispose disposable))
  (swap! !state assoc :extension/disposables []))

(defn cleanup! [!state]
  (when-contexts/set-context!+ !state :calva-mcp-server/active? false)
  (clear-disposables! !state))

(defn register-command!
  [extension-context !state command-id actions]
  (push-disposable! !state (vscode/commands.registerCommand
                            command-id
                            (fn [& args]
                              (ex/dispatch! extension-context (apply ax/enrich-with-args actions args))))))