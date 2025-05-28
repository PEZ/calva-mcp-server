(ns calva-backseat-driver.integrations.calva.editor
  (:require
   ["parinfer" :as parinfer]
   ["vscode" :as vscode]
   [calva-backseat-driver.integrations.calva.api :as calva]
   [promesa.core :as p]))

(defn- get-document-from-path [path]
  (let [uri (vscode/Uri.file path)]
    (.openTextDocument vscode/workspace uri)))

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
     :ranges-object ((get-in calva/calva-api [:ranges ranges-fn-key]) vscode-editor vscode-position)}))

(defn- get-ranges-form-data-by-line
  "Returns the raw Calva API `ranges` object for the top level form at `line-number` (1-indexed),
   in the document at the absolute `file-path`. This is the preferred approach for AI agents."
  [file-path line-number ranges-fn-key]
  (p/let [^js vscode-document (get-document-from-path file-path)
          vscode-editor (get-editor-from-document vscode-document)
          vscode-position (vscode/Position. (dec line-number) 0)]
    {:vscode-document vscode-document
     :ranges-object ((get-in calva/calva-api [:ranges ranges-fn-key]) vscode-editor vscode-position)}))

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
    ((get-in calva/calva-api [:editor :replace]) editor vscode-range new-text)))

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

  :rcf)
