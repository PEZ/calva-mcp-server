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

(defn- filter-clj-kondo-diagnostics
  "Filter diagnostics to only include those from clj-kondo source"
  [diagnostics]
  (->> diagnostics
       (filter #(= "clj-kondo" (.-source %)))))

(defn- get-diagnostics-for-file
  "Get clj-kondo diagnostics for a file"
  [file-path]
  (p/let [uri (vscode/Uri.file file-path)
          diagnostics-raw (vscode/languages.getDiagnostics uri)
          diagnostics diagnostics-raw]
    (filter-clj-kondo-diagnostics diagnostics)))

(defn- find-target-line-by-text
  "Find the actual line number by searching for target text within a window around the initial line.
   Returns the line number (1-indexed) where the target text is found, or nil if not found."
  [^js vscode-document initial-line-number target-text]
  (let [line-count (.-lineCount vscode-document)
        search-window 2  ; search 2 lines above and below
        start-line (max 0 (- initial-line-number search-window 1))  ; convert to 0-indexed and clamp
        end-line (min (dec line-count) (+ initial-line-number search-window 1))]  ; convert to 0-indexed and clamp
    (loop [line-idx start-line]
      (if (<= line-idx end-line)
        (let [line-text (-> vscode-document
                            (.lineAt line-idx)
                            .-text
                            (.trim))]
          (if (= line-text (.trim target-text))
            (inc line-idx)  ; return 1-indexed line number
            (recur (inc line-idx))))
        nil))))

(defn apply-form-edit-by-line-with-text-targeting
  "Apply a form edit by line number with text-based targeting for better accuracy.
   Searches for target-line text within a 2-line window around the specified line number."
  [file-path line-number target-line new-form]
  (-> (p/let [vscode-document (get-document-from-path file-path)
              actual-line-number (if target-line
                                   (find-target-line-by-text vscode-document line-number target-line)
                                   line-number)]
        (if (or (not target-line) actual-line-number)
          (p/let [balance-result (some-> (parinfer/indentMode new-form #js {:partialResult true})
                                         (js->clj :keywordize-keys true))
                  final-line-number (or actual-line-number line-number)
                  form-data (get-ranges-form-data-by-line file-path final-line-number :currentTopLevelForm)
                  diagnostics-before-edit (get-diagnostics-for-file file-path)]
            (if (:success balance-result)
              (p/let [edit-result (edit-replace-range file-path
                                                      (first (:ranges-object form-data))
                                                      (:text balance-result))
                      _ (p/delay 1000) ;; TODO: Consider subscribing on diagnistics changes instead
                      diagnostics-after-edit (get-diagnostics-for-file file-path)]
                (if edit-result
                  {:success true
                   :actual-line-used final-line-number
                   :diagnostics-before-edit diagnostics-before-edit
                   :diagnostics-after-edit diagnostics-after-edit}
                  {:success false
                   :diagnostics-before-edit diagnostics-before-edit}))
              balance-result))
          {:success false
           :error (str "Target line text not found. Expected: '" target-line "' near line " line-number)}))
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

