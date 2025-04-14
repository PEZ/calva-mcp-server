;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns calva-mcp-server.hellos.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn greet [s]
  (str "Hello, " s "!"))

(def input-box-options {:title "Hello Input"
                        :placeHolder "What should we say hello to today?"})

(defn handle-action [state _context action]
  (match action
    [:hello/ax.log-hello name]
    {:ex/db (assoc state :hello/last-greetee name)
     :ex/fxs [[:node/fx.log  "Hello," (str name "!")]]}

    [:hello/ax.say-hello name]
    {:ex/db (assoc state :hello/last-greetee (str "Hello, " name "!"))
     :ex/fxs [[:vscode/fx.show-information-message (str "Hello, " name "!")]]
     :ex/dxs [[:hello/ax.greeting-sent]]}

    [:hello/ax.greeting-sent]
    {:ex/db (assoc state :hello/greeting-sent? true)}

    :else
    {:ex/fxs [[:node/fx.log-error-greeting "Unknown action:" action]]}))