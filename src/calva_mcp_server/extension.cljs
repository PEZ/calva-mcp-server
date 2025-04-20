(ns calva-mcp-server.extension
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.ex.ex :as ex]
   [calva-mcp-server.app.db :as db]
   [calva-mcp-server.extension.life-cycle-helpers :as lc-helpers]))

;;;;; Extension activation entry point

(defn ^:export activate [^js context]
  (js/console.time "activation")
  (js/console.timeLog "activation" "Calva MCP Server activate START")

  (when context
    (swap! db/!app-db assoc :extension/context context))
  (ex/dispatch! context [[:app/ax.init {:app/log-file-uri
                                        (vscode/Uri.joinPath
                                         (.-logUri context) "mcp-server.log")
                                        :app/min-log-level :debug}]
                         [:app/ax.register-command "calva-mcp-server.newHelloDocument" [[:hello/ax.command.hello-doc
                                                                                         {:greetee :ex/action-args%1}]]]
                         [:app/ax.register-command "calva-mcp-server.hello" [[:hello/ax.command.hello
                                                                              {:greetee :ex/action-args%1}]]]
                         [:app/ax.register-command "calva-mcp-server.startServer" [[:mcp/ax.start-server]]]
                         [:app/ax.register-command "calva-mcp-server.stopServer" [[:mcp/ax.stop-server]]]
                         [:app/ax.register-command "calva-mcp-server.openServerLog" [[:mcp/ax.open-server-log]]]
                         [:app/ax.set-when-context :calva-mcp-server/active? true]])

  (js/console.timeLog "activation" "Calva MCP Server activate END")
  (js/console.timeEnd "activation")
  #js {:v1 {}})

(comment
  ;; When you have updated the activate function, cleanup and call activate again
  ;; NB: If you have updated the extension manifest, you will need to restart the extension host instead
  (lc-helpers/cleanup! db/!app-db)
  (activate (:extension/context @db/!app-db))
  :rcf)

;;;;; Extension deactivation entry point

(defn ^:export deactivate []
  (lc-helpers/cleanup! db/!app-db))


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