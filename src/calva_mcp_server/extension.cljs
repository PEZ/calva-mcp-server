(ns calva-mcp-server.extension
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.ex.ex :as ex]
   [calva-mcp-server.app.db :as db]))

(defn- extension-context []
  (:vscode/extension-context @db/!app-db))

(defn- initial-state [^js context]
  {:app/log-file-uri
   (vscode/Uri.joinPath
    (.-logUri context) "mcp-server.log")
   :app/min-log-level :debug})

(defn ^:export activate [^js context]
  (js/console.time "activation")
  (js/console.timeLog "activation" "Calva MCP Server activate START")

  (when-not (extension-context)
    (swap! db/!app-db assoc
           :vscode/extension-context context
           :app/getConfiguration vscode/workspace.getConfiguration))
  (ex/dispatch! context [[:app/ax.activate (initial-state context)]])

  (js/console.timeLog "activation" "Calva MCP Server activate END")
  (js/console.timeEnd "activation")
  #js {:v1 {}})

(comment
  (some-> vscode
          .-workspace
          (.getConfiguration "calva-mcp-server")
          (.get "enableREPLEvaluation"))
  :rcf)

(defn ^:export deactivate []
  (ex/dispatch! (extension-context) [[:app/ax.deactivate]]))

(comment
  (ex/dispatch! (extension-context) [[:app/ax.cleanup]])
  (activate (extension-context))
  :rcf)

;;;;; shadow-cljs hot reload hooks
;; We don't need to do anything here, but it is nice to see that reloading is happening

(defn ^{:dev/before-load true
        :export true}
  before-load []
  (println "shadow-cljs reloading..."))

(defn ^{:dev/after-load true
        :export true}
  after-load []
  (println "shadow-cljs reload complete"))