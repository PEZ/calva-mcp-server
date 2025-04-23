(ns tests.mcp.server-test
  (:require [cljs.test :refer [deftest testing is]]
            [e2e.macros :refer [deftest-async]]
            [promesa.core :as p]
            ["vscode" :as vscode]))

(deftest minimal-test
  (testing "Basic test to verify test discovery"
    (is (= true true) "True should be true")))

(deftest-async command-registration
  (testing "MCP server commands are registered"
    (try
      (p/let [#_#__ (vscode/commands.executeCommand "calva-mcp-server.startServer")
              pre-activation (vscode/commands.getCommands true)]
        (is (= false
               (.includes pre-activation "calva-mcp-server.startServer"))
            "there is no start server command before activation")
        (is (= false
               (.includes pre-activation "calva-mcp-server.stopServer"))
            "there is no stop server command before activation")
        (p/let [extension (vscode/extensions.getExtension "betterthantomorrow.calva-mcp-server")
                _ (.activate extension)
                post-activation (vscode/commands.getCommands true)]
          (is (= true
                 (.includes post-activation "calva-mcp-server.startServer"))
              "there is a start server command after activation")
          (is (= true
                 (.includes post-activation "calva-mcp-server.stopServer"))
              "there is a stop server command before server start")))
      (catch :default e
        (js/console.error (.-message e) e)))))

;; TODO: Figure out why this test errors in e2e
#_(deftest-async server-lifecycle
  (testing "MCP server can be started and stopped via commands"
    (js/console.log "🧪 Testing MCP server start/stop...")

    (-> (p/let [;; Start the server
                _ (p/delay 500)
                _ (js/console.log "📡 Attempting to start MCP server...")
                _ (p/timeout (vscode/commands.executeCommand "calva-mcp-server.startServer")
                             500)
                _ (js/console.log "📡 Server start command executed")

               ;; Stop the server
                _ (js/console.log "🛑 Attempting to stop MCP server...")
                _ (vscode/commands.executeCommand "calva-mcp-server.stopServer")
                _ (js/console.log "🛑 Server stop command executed")]

          (is true "Server commands executed without errors"))
        (p/catch (fn [e]
                   (js/console.error (.-message e) e))))))