(ns calva-mcp-server.extension
  (:require [calva-mcp-server.hellos.fxs :as hellos]
            [calva-mcp-server.extension.db :as db]
            [calva-mcp-server.extension.life-cycle-helpers :as lc-helpers]
            [calva-mcp-server.extension.when-contexts :as when-contexts]))

;;;;; Extension activation entry point

(defn ^:export activate [context]
  (js/console.time "activation")
  (js/console.timeLog "activation" "Calva MCP Server activate START")

  (when context
    (swap! db/!app-db assoc :extension/context context))
  (lc-helpers/register-command! context db/!app-db "calva-mcp-server.newHelloDocument" #'hellos/new-hello-doc-command!+)
  (lc-helpers/register-command! context db/!app-db "calva-mcp-server.hello" #'hellos/hello-command!+)
  (when-contexts/set-context!+ db/!app-db :calva-mcp-server/active? true)

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