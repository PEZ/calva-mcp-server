(ns calva-mcp-server.integrations.calva.api
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn get-document-from-path [path]
  (let [uri (vscode/Uri.file path)]
    (.openTextDocument vscode/workspace uri)))

(def ^:private ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private ^js calvaApi (-> calvaExt
                                .-exports
                                .-v1
                                (js->clj :keywordize-keys true)))

(defn evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and a session key (clj/cljs/cljc), js/undefined means current session."
  [{:ex/keys [dispatch!]} code session ns]
  (p/let [result (-> (p/let [^js evaluation+ ((get-in calvaApi [:repl :evaluateCode])
                                              session code ns)]
                       (dispatch! [[:app/ax.log :debug "[Server] Evaluating code:" code]])
                       {:result (.-result evaluation+)
                        :ns (.-ns evaluation+)
                        :stdout (.-output evaluation+)
                        :stderr (.-errorOutput evaluation+)})
                     (p/catch (fn [err] ; For unknown reasons we end up here if en evaluation throws
                                        ; in the REPL. For now we send the error as the result like this...
                                (dispatch! [[:app/ax.log :debug "[Server] Evaluation failed:"
                                             err]])
                                {:result "nil"
                                 :stderr (pr-str err)})))]
    result))

(defn get-namespace-and-ns-form+
  "Returns a tuple `[ns ns-form]` for the document at `path`, if provided.
   Otherwise will use the currently active editor's document."
  ([]
   ((get-in calvaApi [:document :getNamespaceAndNsForm])))
  ([path]
   (p/let [doc+ (get-document-from-path path)]
     ((get-in calvaApi [:document :getNamespaceAndNsForm]) doc+))))

(defn get-namespace-info+
  "Returns structured namespace information for a given file path.
   This adds a layer over get-namespace-and-ns-form+ that formats
   the data in a more consumable structure."
  [path]
  (p/let [[ns-name ns-form] (get-namespace-and-ns-form+ path)]
    {:namespace ns-name
     :ns-form (pr-str ns-form)
     :file path}))

(comment
  (p/let [[ns ns-form] (get-namespace-and-ns-form+ "/Users/pez/Projects/calva-mcp-server/src/calva_mcp_server/integrations/calva/api.cljs")]
    (def ns ns)
    ;;=> #'calva-mcp-server.integrations.calva.api/ns
    (def ns-form ns-form))
    ;;=> "(ns calva-mcp-server.integrations.calva.api\n  (:require\n   [\"vscode\" :as vscode]\n   [promesa.core :as p]))"

  (p/let [[ns ns-form] (get-namespace-and-ns-form+ "/Users/pez/Projects/calva-mcp-server/test-projects/mini-deps/src/mini/playground.clj")]
    (def ns ns)
    (def ns-form ns-form))
  :rcf)

