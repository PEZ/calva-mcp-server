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

(deftest-async server-lifecycle
  (testing "MCP server can be started and stopped via commands"
    (-> (p/let [_ (p/delay 500) ; Allow log file to have been created
                _ (js/console.log "[server-lifecycle] Attempting to start MCP server...")
                server-info+ (vscode/commands.executeCommand "calva-mcp-server.startServer")
                {:keys [instance port]} (js->clj server-info+ :keywordize-keys true)

                _ (js/console.log "[server-lifecycle] Attempting to stop MCP server...")
                success?+ (vscode/commands.executeCommand "calva-mcp-server.stopServer")]
          (is (not= nil
                    instance)
              "Server instance is something")
          (is (number? port)
              "Server started on a port")
          (is (= true
                 success?+)
              "Server stopped successfully"))
        (p/catch (fn [e]
                   (js/console.error (.-message e) e))))))