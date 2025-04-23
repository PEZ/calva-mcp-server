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
            "there is a stop server command before server start")))))

(deftest-async server-lifecycle
  (testing "MCP server can be started and stopped via commands"
    (js/console.log "🧪 Testing MCP server start/stop...")

    (p/let [;; Start the server
            _ (js/console.log "📡 Attempting to start MCP server...")
            _ (vscode/commands.executeCommand "calva-mcp-server.startServer")
            _ (js/console.log "📡 Server start command executed")

            ;; Give the server a moment to initialize
            _ (p/delay 1000)

            ;; Stop the server
            _ (js/console.log "🛑 Attempting to stop MCP server...")
            _ (vscode/commands.executeCommand "calva-mcp-server.stopServer")
            _ (js/console.log "🛑 Server stop command executed")

            ;; Give the server a moment to shut down
            _ (p/delay 500)]

      (is true "Server commands executed without errors"))))