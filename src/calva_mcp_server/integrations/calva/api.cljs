(ns calva-mcp-server.integrations.calva.api
  (:require
   ["vscode" :as vscode]
   [promesa.core :as p]))

(def ^:private ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private ^js calvaApi (-> calvaExt
                                .-exports
                                .-v1
                                (js->clj :keywordize-keys true)))

(defn evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and a session key (clj/cljs/cljc), js/undefined means current session."
  [{:ex/keys [dispatch!]} code session]
  (p/let [result (-> (p/let [^js evaluation+ ((get-in calvaApi [:repl :evaluateCode])
                                              session code)]
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