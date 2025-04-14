(ns calva-mcp-server.extension.life-cycle-helpers
  (:require ["vscode" :as vscode]
            [calva-mcp-server.extension.when-contexts :as when-contexts]
            [calva-mcp-server.ex.ex :as ex]))

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
  [extension-context !state command-id var]
  (push-disposable! !state (vscode/commands.registerCommand
                            command-id
                            (fn [& args]
                              (js/console.log "BOOM!" command-id args)
                              (apply var ex/dispatch! extension-context !state args)))))