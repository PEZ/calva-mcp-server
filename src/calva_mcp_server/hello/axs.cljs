;; You can't unit test code that use the VSCode API
;; (But check out the e2e test runner)
;;
;; To make your business logic testable, factor it out in namespaces
;; that don't require "vscode".
;;
;; This extension template does not have any business logic, so we'll
;; pretend with these silly examples.

(ns calva-mcp-server.hello.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn greet [s]
  (str "Hello, " s "!"))

(defn handle-action [state _context action]
  (match action
    [:hello/ax.log-hello greetee]
    {:ex/db (assoc state :hello/last-greetee greetee)
     :ex/fxs [[:node/fx.log  "Hello," (str greetee "!")]]}

    [:hello/ax.command.hello arg]
    (let [{:keys [greetee]} arg
          new-state (assoc state :hello/greeting-sent? false)]
      (if greetee
        {:ex/db (assoc new-state :hello/last-greetee greetee)
         :ex/fxs [[:vscode/fx.show-information-message (str "Hello, " greetee "!")]]
         :ex/dxs [[:hello/ax.greeting-sent]]}
        {:ex/db new-state
         :ex/fxs [[:vscode/fx.show-input-box {:title "Hello Input"
                                              :placeHolder "What should we say hello to today?"
                                              :ignoreFocusOut true
                                              :ex/then [[:hello/ax.say-hello :ex/action-args%1]]}]]}))

    [:hello/ax.command.hello-doc arg]
    (let [{:keys [greetee]} arg
          new-state (assoc state :hello/greeting-sent? false)]
      (if greetee
        {:ex/db (assoc new-state :hello/last-greetee greetee)
         :ex/fxs [[:vscode/fx.window.open-text-document {:app/content (str "Hello, " greetee "!")
                                                         :ex/then [[:vscode/ax.show-text-document :ex/action-args]]}]]
         :ex/dxs [[:hello/ax.greeting-sent]]}
        {:ex/db new-state
         :ex/fxs [[:vscode/fx.show-input-box {:title "Hello Input"
                                              :placeHolder "What should we say hello to today?"
                                              :ignoreFocusOut true
                                              :ex/then [[:hello/ax.command.hello-doc {:greetee :ex/action-args}]]}]]}))

    [:hello/ax.say-hello greetee]
    {:ex/db (assoc state :hello/last-greetee greetee)
     :ex/fxs [[:vscode/fx.show-information-message (str "Hello, " greetee "!")]]
     :ex/dxs [[:hello/ax.greeting-sent]]}

    [:hello/ax.greeting-sent]
    {:ex/db (assoc state :hello/greeting-sent? true)}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))