# Calva MCP Server - Event Loop Architecture

This document outlines an event loop architecture for the Calva MCP Server, inspired by the Ex framework pattern demonstrated in the example projects. The architecture follows functional core/imperative shell design principles to keep the system as data-oriented as possible.

## Core Concepts

The event loop architecture is built around these key concepts:

1. **Actions (axs)**: Pure data structures representing something that should happen
2. **Effects (fxs)**: Pure data structures representing side effects to be performed
3. **Application State (db)**: Immutable state managed using atoms
4. **Action Dispatches (dxs)**: New actions to be processed by the system

## System Overview

```
┌─────────────────────────┐
│                         │
│  MCP Message Transport  │
│  (VSCode API / stdio)   │
│                         │
└────────────┬────────────┘
             │
             ▼
┌─────────────────────────┐
│                         │
│  Message Handler        │◄─────┐
│  (Parse/route messages) │      │
│                         │      │
└────────────┬────────────┘      │
             │                   │
             ▼                   │
┌─────────────────────────┐      │
│                         │      │
│  Event Handler          │      │ Action Dispatches
│  (Process actions)      │      │ (dxs)
│                         │      │
└┬───────────┬────────────┘      │
 │           │                   │
 │           │                   │
 ▼           ▼                   │
┌────────────┴────────────┐      │
│                         │      │
│  Effect Handler         ├──────┘
│  (Perform side effects) │
│                         │
└─────────────────────────┘
```

## Core Components

### 1. Action Handling (ax.cljs)

Actions are pure data structures that represent something that should happen. Each action is a vector with a namespaced keyword as the first element, followed by any parameters:

```clojure
(defn handle-action [state ctx action]
  (let [enriched-action (-> action
                            (enrich-action-from-context ctx)
                            (enrich-action-from-state state))]
    (match enriched-action
      ;; MCP Protocol Actions
      [:mcp/ax.register-server]
      {:ex/fxs [[:mcp/fx.register-server]]}

      [:mcp/ax.execute-tool tool-name params]
      {:ex/fxs [[:mcp/fx.execute-tool tool-name params]]}

      ;; REPL Evaluation Actions
      [:repl/ax.evaluate code session]
      {:ex/fxs [[:repl/fx.evaluate code session]]}

      [:repl/ax.handle-result result]
      {:ex/db (assoc state :last-eval-result result)
       :ex/fxs [[:mcp/fx.send-result result]]}

      ;; System Actions
      [:system/ax.init]
      {:ex/db (assoc state :initialized true)
       :ex/dxs [[:mcp/ax.register-server]]})))
```

### 2. Effect Handling (fx.cljs)

Effects are pure data structures that represent side effects to be performed. The effect handler is the only place where we interact with the outside world:

```clojure
(defn perform-effect! [event-handler ctx effect]
  (match effect
    ;; MCP Protocol Effects
    [:mcp/fx.register-server]
    (p/let [result (register-mcp-server)]
      (event-handler ctx [[:system/ax.log "MCP server registered" result]]))

    [:mcp/fx.execute-tool tool-name params]
    (case tool-name
      "evaluate-clojure" (event-handler ctx [[:repl/ax.evaluate (:code params) (:session params)]])
      (event-handler ctx [[:system/ax.log (str "Unknown tool: " tool-name)]]))

    [:mcp/fx.send-result result]
    (send-result-to-mcp-client result)

    ;; REPL Effects
    [:repl/fx.evaluate code session]
    (p/let [result (evaluate-code-in-calva code session)]
      (event-handler ctx [[:repl/ax.handle-result result]]))

    ;; System Effects
    [:system/fx.log & args]
    (apply js/console.log args)))
```

### 3. Event Handler (event_handler.cljs)

The event handler orchestrates the flow of actions and effects, maintaining the application state:

```clojure
(defn event-handler [ctx actions]
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @db/!state ctx actions)]
    (when db
      (reset! db/!state db))
    (when dxs
      (event-handler ctx dxs))
    (when fxs
      (doseq [fx fxs]
        (fx/perform-effect! event-handler ctx fx)))))
```

## MCP Message Flow

1. **Incoming Messages**:
   - MCP client (GitHub Copilot) sends a message to execute a tool
   - Message is parsed and converted to an action: `[:mcp/ax.execute-tool "evaluate-clojure" {:code "(+ 1 2)" :session "clj"}]`
   - Action is dispatched to the event handler

2. **Processing**:
   - Event handler processes the action and generates an effect: `[:repl/fx.evaluate "(+ 1 2)" "clj"]`
   - Effect handler executes the code in Calva's REPL and generates a new action with the result: `[:repl/ax.handle-result {:value "3" :type "number"}]`
   - Event handler updates the state and generates a new effect: `[:mcp/fx.send-result {:value "3" :type "number"}]`
   - Effect handler sends the result back to the MCP client

## Enrichment Process

To maintain pure data vectors as actions and effects, we use an enrichment process:

1. `enrich-action-from-context`: Replaces context-related keywords with actual values
2. `enrich-action-from-state`: Resolves references to state using patterns like `[:db/get :some-key]`

This allows us to write actions as pure data vectors while still having access to the necessary context at runtime.

## Benefits of This Architecture

1. **Pure Functions**: The core logic is implemented as pure functions that transform data
2. **Testability**: Actions and their handlers can be easily tested without mocking
3. **Separation of Concerns**: Clear separation between state management, core logic, and side effects
4. **Traceability**: All system behavior is expressed as explicit data structures
5. **Extensibility**: Easy to add new actions and effects without modifying existing code

## Implementation Considerations

1. **State Management**: Use atoms for holding application state
2. **Effect Coordination**: Consider adding specialized effects for handling async operations
3. **Error Handling**: Add specific actions and effects for error handling
4. **Logging/Debugging**: Implement a comprehensive logging system using actions and effects
5. **Integration with VS Code**: Create specific effects for VS Code API interactions

This architecture provides a solid foundation for building the Calva MCP Server while maintaining the functional and data-oriented approach that your example projects demonstrate.