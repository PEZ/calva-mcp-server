(ns calva-mcp-server.extension
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.ex.ex :as ex]
   [calva-mcp-server.app.db :as db]))

(defn- initial-state [^js context]
  {:app/log-file-uri
   (vscode/Uri.joinPath
    (.-logUri context) "mcp-server.log")
   :app/min-log-level :debug})

(defn ^:export activate [^js context]
  (js/console.time "activation")
  (js/console.timeLog "activation" "Calva MCP Server activate START")

  (swap! db/!app-db assoc :extension/context context)
  (ex/dispatch! context [[:app/ax.activate (initial-state context)]])

  (js/console.timeLog "activation" "Calva MCP Server activate END")
  (js/console.timeEnd "activation")
  #js {:v1 {}})


(defn ^:export deactivate []
  (ex/dispatch! (:extension/context @db/!app-db) [:app/ax.deactivate]))

(comment
  (deactivate)
  (activate (:extension/context @db/!app-db))
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