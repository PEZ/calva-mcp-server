(ns calva-mcp-server.integrations.calva.api
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn get-document-from-path [path]
  (let [uri (vscode/Uri.file path)]
    (.openTextDocument vscode/workspace uri)))

(def ^:private ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private calva-api (-> calvaExt
                             .-exports
                             .-v1
                             (js->clj :keywordize-keys true)))

(def ^:private no-ns-eval-note
  "When evaluating without providing a namespace argument the evaluation is performed in the `user` namespace. Most often this is not what you want, and instead you should be evaluating providing the namespace argument. If it is the first time you are using a namespace, evaluate its ns-form first.")

(def ^:private empty-result-note
  "Not expecting a empty string as a result? If it is the first time you are using a namespace, evaluate its ns-form in the `user` namespace first.")


(def ^:private error-result-note
  "* clj: Evaluating `*e` will give your information about the error.
   * cljs: Evaluating `(.-stack *e), gives you a stack trace")

(defn evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and a session key (clj/cljs/cljc), js/undefined means current session."
  [{:ex/keys [dispatch!]
    :calva/keys [code repl-session-key ns]}]
  (p/let [evaluate (get-in calva-api [:repl :evaluateCode])
          result (-> (p/let [^js evaluation+ (if ns
                                               (evaluate repl-session-key code ns)
                                               (evaluate repl-session-key code))]
                       (dispatch! [[:app/ax.log :debug "[Server] Evaluating code:" code]])
                       (cond-> {:result (.-result evaluation+)
                                :ns (.-ns evaluation+)
                                :stdout (.-output evaluation+)
                                :stderr (.-errorOutput evaluation+)
                                :session-key (.-sessionKey evaluation+)
                                :note "Remember to check the output tool now and then to see what's happening in the application."}
                         (.-error evaluation+)
                         (merge {:error (.-error evaluation+)
                                 :stacktrace (.-stacktrace evaluation+)})

                         (not ns)
                         (merge {:note no-ns-eval-note})

                         (= "" (.-result evaluation+))
                         (merge {:note empty-result-note})))
                     (p/catch (fn [err] ; For unknown reasons we end up here if en evaluation throws
                                        ; in the REPL. For now we send the error as the result like this...
                                (dispatch! [[:app/ax.log :debug "[Server] Evaluation failed:"
                                             err]])
                                {:result "nil"
                                 :stderr (pr-str err)
                                 :note error-result-note})))]
    (clj->js result)))

(def description-clojure-docs
  "Returns clojuredocs.org info on `symbol`.")

(defn get-clojuredocs+ [{:ex/keys [dispatch!]
                         :calva/keys [clojure-symbol]}]
  (dispatch! [[:app/ax.log :debug "[Server] Getting clojuredocs for:" clojure-symbol]])
  ((get-in calva-api [:info :getClojureDocsDotOrg]) clojure-symbol "user"))

(defn exists-get-clojuredocs? [] (boolean (get-in calva-api [:info :getClojureDocsDotOrg])))

(def description-symbol-info
  "Returns info on the `symbol` as resolved in `namespace`.")

(defn get-symbol-info+ [{:ex/keys [dispatch!]
                         :calva/keys [clojure-symbol ns repl-session-key]}]
  (dispatch! [[:app/ax.log :debug "[Server] Getting clojuredocs for:" clojure-symbol]])
  ((get-in calva-api [:info :getSymbolInfo]) clojure-symbol repl-session-key ns))

(defn exists-get-symbol-info? [] (boolean (get-in calva-api [:info :getSymbolInfo])))

(defn subscribe-to-output [{:ex/keys [dispatch!]
                            :calva/keys [on-output]}]
  ((get-in calva-api [:repl :onOutputLogged])
   (fn [message]
     (dispatch! (conj on-output (js->clj message :keywordize-keys true))))))

(def description-get-output
  "CRITICAL: Returns REPL output since `since-line`. Start with since-line=0 and use the last line number from previous output for subsequent calls. This is your window into the running application.")

(defn get-output [{:ex/keys [dispatch!]
                   :calva/keys [since-line]}]
  (clj->js
   (dispatch! nil [[:app/ax.log :debug "[Server] Getting getting output since line:" since-line]
                   [:calva/ax.get-output since-line]])))

(defn exists-on-output? [] (boolean (get-in calva-api [:repl :onOutputLogged])))

(comment
  (p/let [info (get-symbol-info+ {:ex/dispatch! (comp pr-str println)
                                  :calva/clojure-symbol "clojure.core/reductions"
                                  :calva/repl-session-key "clj"
                                  :calva/ns "user"})]
    (def info info))
  (js->clj info :keywordize-keys true)

  (p/let [docs (get-clojuredocs+ {:ex/dispatch! (comp pr-str println)
                                  :calva/clojure-symbol "clojure.core/reductions"})]
    (def docs docs))
  (js->clj docs :keywordize-keys true)

  :rcf)

