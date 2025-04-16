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
    (p/let [commands (vscode/commands.getCommands true)
            commands-set (set commands)]
      (is (contains? commands-set "calva-mcp-server.startServer")
          "Start server command should be registered")
      (is (contains? commands-set "calva-mcp-server.stopServer")
          "Stop server command should be registered"))))

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