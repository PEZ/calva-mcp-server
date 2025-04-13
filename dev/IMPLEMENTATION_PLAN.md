# Calva MCP Server - Implementation Plan

Based on our [decision to explore VS Code's MCP integration](./DECISION_LOG.md#2025-04-13-mcp-server-integration-approach-for-vs-code), we'll start with an exploratory spike before committing to a specific implementation approach.

### Spike Goals

1. Determine how VS Code extensions register as MCP servers
2. Identify how GitHub Copilot discovers and interacts with MCP servers
3. Understand the message format and communication patterns
4. Assess the maturity and stability of VS Code's MCP integration APIs

### Spike Implementation

```clojure
;; In calva-mcp-server.mcp.spike

(ns calva-mcp-server.mcp.spike
  (:require ["vscode" :as vscode]
            [calva-mcp-server.extension.db :as db]
            [promesa.core :as p]))

;; Function to detect if GitHub Copilot is installed
(defn copilot-installed? []
  (some? (vscode/extensions.getExtension "GitHub.copilot")))

;; Function to detect if GitHub Copilot Chat is installed
(defn copilot-chat-installed? []
  (some? (vscode/extensions.getExtension "GitHub.copilot-chat")))

;; MCP tool for REPL evaluation
(def evaluate-tool
  {:name "evaluate-clojure"
   :description "Evaluates Clojure/ClojureScript code in the REPL"
   :parameters [{:name "code"
                 :type "string"
                 :description "Clojure/ClojureScript code to evaluate"}
                {:name "session"
                 :type "string"
                 :description "REPL session (clj or cljs)"
                 :required false}]
   :handler (fn [params]
              ;; Implementation will connect to Calva API
              {:result "Mock result for spike testing"})})

;; Function to register our MCP server with VS Code/Copilot
(defn register-mcp-server []
  (let [api (.. vscode -commands)]
    ;; We'll explore different VS Code APIs to find the correct one
    ;; for registering MCP servers
    (p/let [registration-result (api.executeCommand "mcp.registerServer"
                                                  {:name "Calva MCP Server"
                                                   :version "0.0.1"
                                                   :tools [evaluate-tool]})]
      (when registration-result
        (swap! db/!state assoc :mcp-server-registered true)
        (vscode/window.showInformationMessage "Calva MCP Server registered successfully"))

      ;; Return registration result for logging/debugging
      registration-result)))

;; Start the spike exploration
(defn start-spike! []
  (p/let [copilot? (copilot-installed?)
          copilot-chat? (copilot-chat-installed?)]
    (vscode/window.showInformationMessage
     (str "Copilot installed: " copilot? ", Copilot Chat installed: " copilot-chat?))

    ;; Attempt registration
    (when (or copilot? copilot-chat?)
      (p/catch
       (register-mcp-server)
       (fn [err]
         (vscode/window.showErrorMessage
          (str "Failed to register Calva MCP Server: " (.-message err)))
         (js/console.error "MCP registration error:" err))))))
```

### Exploration Areas

1. **API Discovery**:
   - Research VS Code proposed APIs related to MCP
   - Look at GitHub Copilot source/documentation for MCP integration points
   - Experiment with different command names for MCP server registration

2. **Message Format Exploration**:
   - Intercept and log messages between Copilot and our server
   - Document the actual format used in VS Code (vs. standard MCP)

3. **Integration Mechanics**:
   - Test whether registration happens at extension activation
   - Determine lifecycle of MCP servers in VS Code

### Spike Success Criteria

The spike will be considered successful if we can:

1. Determine the correct mechanism for MCP server registration in VS Code
2. Successfully register our server with GitHub Copilot
3. Receive and respond to at least one tool invocation
4. Document the message format and protocol details

### Post-Spike Decision

After completing the spike, we will:

1. Update our implementation plan based on findings
2. Document VS Code's MCP integration approach
3. Choose the appropriate transport mechanism
4. Implement the actual MCP server using the knowledge gained

This exploratory approach ensures we build on solid understanding rather than assumptions about VS Code's evolving MCP support.

## Next Implementation Steps

1. Create the basic directory structure for our MCP server implementation
2. Implement the tool and parameter data structures
3. Create the JSON-RPC message handling logic
4. Implement the stdio transport mechanism
5. Connect to Calva's API for REPL evaluation
6. Test with an MCP client like Claude Desktop

This implementation plan provides a clear path forward while following the patterns established by Modex, adapted for our ClojureScript and VS Code extension environment.