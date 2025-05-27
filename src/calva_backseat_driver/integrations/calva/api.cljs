(ns calva-backseat-driver.integrations.calva.api
  (:require
   ["parinfer" :as parinfer]
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

(defn when-calva-activated [{:ex/keys [dispatch! then]}]
  (let [!interval-id (atom nil)]
    (reset! !interval-id (js/setInterval (fn []
                                           (when (.-isActive calvaExt)
                                             (js/clearInterval @!interval-id)
                                             (dispatch! then)))
                                         100))))

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
                                :session-key (.-replSessionKey evaluation+)
                                :note "Remember to check the output tool now and then to see what's happening in the application."}
                         (.-error evaluation+)
                         (merge {:error (.-error evaluation+)
                                 :stacktrace (.-stacktrace evaluation+)})

                         (not ns)
                         (merge {:note no-ns-eval-note})

                         (= "" (.-result evaluation+))
                         (merge {:note empty-result-note})))
                     (p/catch (fn [err]
                                (dispatch! [[:app/ax.log :debug "[Server] Evaluation failed:"
                                             err]])
                                {:result "nil"
                                 :stderr (pr-str err)
                                 :note error-result-note})))]
    (clj->js result)))

(defn get-clojuredocs+ [{:ex/keys [dispatch!]
                         :calva/keys [clojure-symbol]}]
  (dispatch! [[:app/ax.log :debug "[Server] Getting clojuredocs for:" clojure-symbol]])
  ((get-in calva-api [:info :getClojureDocsDotOrg]) clojure-symbol "user"))

(defn exists-get-clojuredocs? [] (boolean (get-in calva-api [:info :getClojureDocsDotOrg])))

(defn get-symbol-info+ [{:ex/keys [dispatch!]
                         :calva/keys [clojure-symbol ns repl-session-key]}]
  (dispatch! [[:app/ax.log :debug "[Server] Getting symbol info for:" clojure-symbol]])
  ((get-in calva-api [:info :getSymbolInfo]) clojure-symbol repl-session-key ns))

(defn exists-get-symbol-info? [] (boolean (get-in calva-api [:info :getSymbolInfo])))

(defn subscribe-to-output [{:ex/keys [dispatch!]
                            :calva/keys [on-output]}]
  ((get-in calva-api [:repl :onOutputLogged])
   (fn [message]
     (dispatch! (conj on-output (js->clj message :keywordize-keys true))))))

(defn get-output [{:ex/keys [dispatch!]
                   :calva/keys [since-line]}]
  (clj->js
   (dispatch! [[:app/ax.log :debug "[Server] Getting getting output since line:" since-line]
               [:calva/ax.get-output since-line]])))

(defn exists-on-output? [] (boolean (get-in calva-api [:repl :onOutputLogged])))

(defn get-editor-from-document [vscode-document]
  (let [visible-editor (->> vscode/window.visibleTextEditors
                            (filter (fn [doc] (= (.-document doc) vscode-document)))
                            first)]
    (if visible-editor
      visible-editor
      (.showTextDocument vscode/window vscode-document))))

(defn- get-editor-from-file-path [file-path]
  (p/let [vscode-document (get-document-from-path file-path)]
    (get-editor-from-document vscode-document)))

(defn- get-ranges-form-data
  "Returns the raw Calva API `ranges` object for the top level form at `position`,
   an index into the document at the absolute `file-path`"
  [file-path position ranges-fn-key]
  (p/let [^js vscode-document (get-document-from-path file-path)
          vscode-editor (get-editor-from-document vscode-document)
          vscode-position (.positionAt vscode-document position)]
    {:vscode-document vscode-document
     :ranges-object ((get-in calva-api [:ranges ranges-fn-key]) vscode-editor vscode-position)}))

(defn- get-ranges-form-data-by-line
  "Returns the raw Calva API `ranges` object for the top level form at `line-number` (1-indexed),
   in the document at the absolute `file-path`. This is the preferred approach for AI agents."
  [file-path line-number ranges-fn-key]
  (p/let [^js vscode-document (get-document-from-path file-path)
          vscode-editor (get-editor-from-document vscode-document)
          vscode-position (vscode/Position. (dec line-number) 0)]
    {:vscode-document vscode-document
     :ranges-object ((get-in calva-api [:ranges ranges-fn-key]) vscode-editor vscode-position)}))

(defn- get-range-and-form
  "Returns the range and the form from the Calva API `ranges` object as a tuple
   `[[start end] form-string]` where `start` and `end` are indexes into the document text."
  [{:keys [^js ranges-object ^js vscode-document]}]
  (let [[^js vscode-range form-string] ranges-object
        start-offset (.offsetAt vscode-document (.-start vscode-range))
        end-offset (.offsetAt vscode-document (.-end vscode-range))]
    [[start-offset end-offset] form-string]))

(defn- edit-replace-range [file-path vscode-range new-text]
  (p/let [^js editor (get-editor-from-file-path file-path)]
    (.revealRange editor vscode-range)
    ((get-in calva-api [:editor :replace]) editor vscode-range new-text)))

;; TODO: Figure out how to handle writing new files
(defn apply-form-edit [file-path position new-form]
  (-> (p/let [balance-result (some-> (parinfer/indentMode  new-form #js {:partialResult true})
                                     (js->clj :keywordize-keys true))
              form-data (get-ranges-form-data file-path position :currentTopLevelForm)]
        (if (:success balance-result)
          (p/let [edit-result (edit-replace-range file-path
                                                  (first (:ranges-object form-data))
                                                  (:text balance-result))]
            (if edit-result
              {:success true
               :note "Please use the lint/problems/error tool to check if the edits generated or fixed problems."}
              {:success false}))
          balance-result))
      (p/catch (fn [e]
                 {:success false
                  :error (.-message e)}))))

(defn apply-form-edit-by-line
  "Apply a form edit by line number instead of character position.
   This is the preferred approach for AI agents as they can see and reason about line numbers."
  [file-path line-number new-form]
  ;; Trigger hot reload
  (-> (p/let [balance-result (some-> (parinfer/indentMode new-form #js {:partialResult true})
                                     (js->clj :keywordize-keys true))
              form-data (get-ranges-form-data-by-line file-path line-number :currentTopLevelForm)]
        (if (:success balance-result)
          (p/let [edit-result (edit-replace-range file-path
                                                  (first (:ranges-object form-data))
                                                  (:text balance-result))
                  uri (vscode/Uri.file file-path)
                  diagnostics-raw (vscode/languages.getDiagnostics uri)
                  diagnostics (js->clj diagnostics-raw :keywordize-keys true)]
            (if edit-result
              {:success true
               :diagnostics diagnostics}
              {:success false}))
          balance-result))
      (p/catch (fn [e]
                 {:success false
                  :error (.-message e)}))))

(comment
  (p/let [ctf-data (get-ranges-form-data
                    "/Users/pez/Projects/calva-mcp-server/test-projects/example/src/mini/playground.clj"
                    214
                    :currentTopLevelForm)]
    (def ctf-data ctf-data)
    (edit-replace-range "/Users/pez/Projects/calva-mcp-server/test-projects/example/src/mini/playground.clj"
                        (first (:ranges-object ctf-data))
                        "foo")
    (get-range-and-form ctf-data))

  (p/let [edit-result (apply-form-edit "/Users/pez/Projects/calva-mcp-server/test-projects/example/src/mini/playground.clj"
                                       214
                                       "(foo")]
    (def edit-result edit-result))


  (.-line (vscode/Position. 0))
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

