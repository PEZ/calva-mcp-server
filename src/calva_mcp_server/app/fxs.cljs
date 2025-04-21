(ns calva-mcp-server.app.fxs
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.mcp.logging :as logging]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [dispatch! ^js context effect]
  (match effect
    [:app/fx.init-logging options]
    (logging/init!+ options)

    [:app/fx.log options level messages]
    (let [min-level (:app/min-log-level options :debug)
          levels {:error 0, :warn 1, :info 2, :debug 3}]
      (when (<= (levels level) (levels min-level))
        (apply logging/log! (assoc options :ex/dispatch! dispatch!) level messages)))

    [:app/fx.register-command command-id actions]
    (let [disposable (vscode/commands.registerCommand
                      command-id
                      (fn [& args]
                        (dispatch!
                         context
                         (ax/enrich-with-args actions
                                              (js->clj args :keywordize-keys true)))))]
      (.push (.-subscriptions context) disposable)
      (dispatch! context [[:db/ax.update-in [:extension/disposables] conj disposable]]))

    [:app/fx.clear-disposables disposables]
    (doseq [^js disposable disposables]
      (.dispose disposable))

    :else
    (js/console.warn "Unknown extension effect:" (pr-str effect))))