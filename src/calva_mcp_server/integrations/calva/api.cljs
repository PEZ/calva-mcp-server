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
  "Not expecting a empty string as a result? If it is the first time you are using a namespace, evaluate its ns-form first.")

(defn evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and a session key (clj/cljs/cljc), js/undefined means current session."
  [{:ex/keys [dispatch!]
    :calva/keys [code session ns]}]
  (p/let [evaluate (get-in calva-api [:repl :evaluateCode])
          result (-> (p/let [^js evaluation+ (if ns
                                               (evaluate session code ns)
                                               (evaluate session code))]
                       (dispatch! [[:app/ax.log :debug "[Server] Evaluating code:" code]])
                       (merge {:result (.-result evaluation+)
                               :ns (.-ns evaluation+)
                               :stdout (.-output evaluation+)
                               :stderr (.-errorOutput evaluation+)}
                              (cond
                                (not ns)
                                {:note no-ns-eval-note}

                                (= "" (.-output evaluation+))
                                {:note empty-result-note})))
                     (p/catch (fn [err] ; For unknown reasons we end up here if en evaluation throws
                                        ; in the REPL. For now we send the error as the result like this...
                                (dispatch! [[:app/ax.log :debug "[Server] Evaluation failed:"
                                             err]])
                                {:result "nil"
                                 :stderr (pr-str err)
                                 :note "Think a bit about why your evaluation resulted i an exception brefore proceeding. Consider asking your pair programmer (the user) about it, relaying what error you got."})))]
    result))

(def description-clojure-docs
  "Returns clojuredocs.org info on `symbol`.")

(defn get-clojuredocs+
  [{:ex/keys [dispatch!]
    :calva/keys [clojure-symbol]}]
  (dispatch! [[:app/ax.log :debug "[Server] Getting clojuredocs for:" clojure-symbol]])
  ((get-in calva-api [:info :getClojureDocsDotOrg]) clojure-symbol "user"))

(def exists-get-clojuredocs? (boolean (get-in calva-api [:info :getClojureDocsDotOrg])))

(def description-symbol-info
  "Returns info on the `symbol` as resolved in `namespace`.")

(defn get-symbol-info+ [ns clojure-symbol]
  ((get-in calva-api [:info :getSymbolInfo]) clojure-symbol ns))

(def exists-get-symbol-info? (boolean (get-in calva-api [:info :getSymbolInfo])))


(comment
  (p/let [info (get-symbol-info+ "user" "clojure.core/reductions")]
    (def info info))
  (js->clj info :keywordize-keys true)

  (p/let [docs (get-clojuredocs+ {:ex/dispatch! (comp pr-str println)
                                  :calva/clojure-symbol "clojure.core/reductions"})]
    (def docs docs))
  (js->clj docs :keywordize-keys true)

  :rcf)

