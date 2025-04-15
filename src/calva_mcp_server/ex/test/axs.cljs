;; These are not actions really used by the application/extension
;; They are meant for testing the Ex implementation itself

(ns calva-mcp-server.ex.test.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:ex-test/ax.log-message message]
    {:ex/db (assoc state
                   :ex-test/last-message message
                   :ex-test/ax.message-logged false)
     :ex/fxs [[:node/fx.log  "ex-test" message]]
     :ex/dxs [[:ex-test/ax.message-logged]]}

    [:ex-test/ax.message-logged]
    {:ex/db (assoc state :ex-test/ax.message-logged true)}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))