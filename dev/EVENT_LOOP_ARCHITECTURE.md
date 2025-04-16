# Calva MCP Server - Ex Architecture

This document describes the Ex (Event Dispatch) architecture used in the Calva MCP Server project. Ex is a micro framework that implements a functional core/imperative shell design, emphasizing immutability, pure functions, and unidirectional data flow.

## Core Concepts

The Ex architecture revolves around these key concepts:

1. **Actions (ax)**: Pure data structures representing operations to perform, structured as vectors with a namespaced keyword identifier and parameters
2. **Effects (fx)**: Data structures representing controlled side effects 
3. **Action Enrichment**: System for transforming pure data structures with contextual information at runtime
4. **Dispatch**: Mechanism for action processing, new state creation, and triggering effects
5. **Application State**: Immutable data managed through atoms
6. **Action Dispatches (dxs)**: New actions to process after the current action completes

## Evolutionary Design

The Ex framework is embedded directly in the project source (in the `calva_mcp_server.ex` namespace) rather than imported as a library. This is intentional, allowing the framework to evolve alongside the project's needs. As requirements change, the framework can be extended with:

- New enrichment strategies
- Additional effect types
- Modified dispatch behaviors
- Domain-specific optimizations

This evolutionary approach ensures the architecture remains flexible and tailored to the specific needs of the Calva MCP Server project.

## System Flow

The Ex architecture follows a unidirectional data flow pattern with clear separation between pure functions and side effects:

1. `dispatch!` function receives an actions collection and calls `handle-actions`
2. `handle-actions` processes each action sequentially with the latest state
3. Each individual action goes through:
   - Enrichment (context and state)
   - Domain-specific handling
   - Result generation with `:ex/db`, `:ex/fxs`, `:ex/dxs` keys
4. Results are accumulated into a single batch result
5. The dispatcher then:
   - Updates application state via `reset!` (a controlled side effect)
   - Processes any resulting dispatched actions (`:ex/dxs`)
   - Executes effects (`:ex/fxs`)

## Core Components

### 1. Action Handler (ax.cljs)

The action handler system processes actions by:
1. Enriching actions with context and state
2. Routing actions to domain-specific handlers based on namespace
3. Returning action results with new state and effects

```clojure
(defn handle-action [state context [action-kw :as action]]
  (let [enriched-action (-> action
                            (enrich-action-from-context context)
                            (enrich-action-from-state state))]
    (match (namespace action-kw)
      "hello"  (hello-axs/handle-action state context enriched-action)
      "vscode" (vscode-axs/handle-action state context enriched-action)
      "node"   (node-axs/handle-action state context enriched-action)
      "ex-test"   (ex-test-axs/handle-action state context enriched-action)
      :else {:fxs [[:node/fx.log-error "Unknown action namespace for action:" (pr-str action)]]})))
```

Action handlers return maps with these keys:
- `:ex/db` - New application state
- `:ex/fxs` - Effects to perform
- `:ex/dxs` - Additional actions to dispatch

### 2. Effect Handler (fx.cljs)

The effect system performs side effects through a handler that:
1. Routes effects to domain-specific handlers based on namespace
2. Executes side effects in a controlled manner
3. Optionally chains new actions via the dispatcher

```clojure
(defn perform-effect! [dispatch! context [effect-kw :as effect]]
  (match (namespace effect-kw)
    "node" (node-fxs/perform-effect! dispatch! context effect)
    "vscode" (vscode-fxs/perform-effect! dispatch! context effect)
    :else (js/console.warn "Unknown effect namespace:" (pr-str effect))))
```

### 3. Dispatcher (ex.cljs)

The dispatcher orchestrates the overall flow:

```clojure
(defn dispatch! [extension-context actions]
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @db/!app-db extension-context actions)]
    (when db
      ;; State replacement is kept outside the pure action handlers
      ;; It's treated as a controlled side effect
      (reset! db/!app-db db))
    (when (seq dxs)
      (dispatch! extension-context dxs))
    (when (seq fxs)
      (last (map (fn [fx]
                   (fx/perform-effect! dispatch! extension-context fx))
                 fxs)))))
```

The key aspects of this design:
1. The dispatcher receives a collection of actions, not just a single action
2. `handle-actions` processes and accumulates results from multiple actions
3. State replacement via `reset!` is intentionally kept outside the pure action handlers
4. The dispatcher handles the recursive processing of any dispatched actions (dxs)
5. Effects are executed after state replacement and before processing dispatched actions

## Action Enrichment

The architecture includes action enrichment to support pure data expressions:

### Context Enrichment
Replaces keywords namespaced with "context" with values from the JS context object:

```clojure
(defn- enrich-action-from-context [action context]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (cond (= "context" (namespace x)) (let [path (string/split (name x) #"\.")]
                                           (js-get-in context path))
             :else x)
       x))
   action))
```

### State Enrichment
Replaces `[:db/get key]` vectors with values from the application state:

```clojure
(defn- enrich-action-from-state [action state]
  (walk/postwalk
   (fn [x]
     (cond
       (and (vector? x) (= :db/get (first x)))
       (get state (second x))

       :else x))
   action))
```

### Arguments Enrichment
Supports passing arguments between effects and subsequent actions:

```clojure
(defn enrich-with-args [actions args]
  (walk/postwalk
   (fn [x]
     (cond
       (= :ex/action-args x) args

       (and (keyword? x)
            (= "ex" (namespace x))
            (.startsWith (name x) "action-args%"))
       (let [[_ n] (re-find #"action-args%(\d+)" (name x))]
         (nth args (dec (parse-long n))))

       :else x))
   actions))
```

## Async and Dispatch Capabilities

The Ex architecture provides mechanisms for handling asynchronous flows and dispatching actions from effects:

### Direct Dispatch from Effects

Effect handlers receive the `dispatch!` function, allowing them to directly trigger new actions:

```clojure
(defn perform-effect! [dispatch! context effect]
  (match effect
    [:some/fx.async-operation]
    (-> (js/Promise.resolve "result")
        (.then (fn [result]
                 (dispatch! context [[:some/ax.handle-result result]]))))
    
    ;; ...other effects
    ))
```

This capability enables:
- Debounced dispatches
- Batched updates
- Conditional action flows
- Complex async sequences

### Promise Continuation with :ex/then

A powerful pattern used throughout the framework is the `:ex/then` option for effects that return promises:

```clojure
;; In an action handler:
{:ex/fxs [[:vscode/fx.show-input-box 
           {:title "Enter Name"
            :ex/then [[:hello/ax.greet :ex/action-args]]}]]}

;; In the effect handler:
(-> (vscode/window.showInputBox (clj->js options))
    (.then (fn [input]
             (when input
               (dispatch! context (ax/enrich-with-args then input))))))
```

When the promise resolves, the result is passed to `enrich-with-args` which injects it into the action template using placeholders:

- `:ex/action-args` - The entire result
- `:ex/action-args%n` - Access specific indices in collection results, where n is any positive number (e.g., `:ex/action-args%1`, `:ex/action-args%2`, `:ex/action-args%22`)

The indexed placeholders (`%n` syntax) let you access specific elements of collection results, supporting both simple cases and more complex scenarios where effects produce multiple values that need to be addressed individually.

### Nested Async Flows

The architecture supports nested async operations:

```clojure
[:vscode/fx.show-input-box 
 {:ex/then [[:vscode/fx.open-text-document 
             {:content :ex/action-args
              :ex/then [[:vscode/ax.show-text-document :ex/action-args]]}]]}]
```

This creates a clean, declarative way to express complex async workflows while maintaining the pure data representation at each step.

## Action Processing Pipeline

The action processing pipeline shows how actions flow through the system:

```
[action1, action2, ...] → handle-actions → {
  for each action:
    action → enrich-action → domain-specific handle-action → individual result
  
  accumulate individual results into:
    {:ex/db new-state, :ex/fxs effects, :ex/dxs dispatched-actions}
}
```

The `handle-actions` function is central to this process:

```clojure
(defn handle-actions [state context actions]
  (reduce (fn [{state :ex/db :as acc} action]
            (let [{:ex/keys [db fxs dxs]} (handle-action state context action)]
              (cond-> acc
                db (assoc :ex/db db)
                dxs (update :ex/dxs into dxs)
                fxs (update :ex/fxs into fxs))))
          {:ex/db state
           :ex/fxs []
           :ex/dxs []}
          (remove nil? actions)))
```

This function:
1. Takes the current state, context, and a collection of actions
2. Processes each action sequentially, passing the latest state to each action 
3. Accumulates results (db, fxs, dxs) into a single result map
4. Ignores nil actions, allowing conditional inclusion in action collections

## Domain-Specific Handlers

The architecture uses namespaced actions and effects to create clear domain boundaries:

1. **hello**: Example greeting functionality
2. **vscode**: VS Code-specific operations
3. **node**: Node.js operations like logging

Each domain has its own action and effect handlers, following a consistent pattern.

## Async Operations

The architecture supports async operations primarily through effect handlers. For example, VS Code input boxes return Promises, which are then used to trigger follow-up actions:

```clojure
[:vscode/fx.show-input-box {:title "Hello Input"
                           :ex/then [[:hello/ax.say-hello :ex/action-args]]}]
```

This creates a clean way to handle asynchronous flows while maintaining pure data structures for actions and effects.

## Benefits

1. **Functional and Data Oriented**: Business logic is implemented in pure functions that transform data
2. **Predictable Dataflow**: All state is immutable; new state is created rather than mutating existing state
3. **Improved Testability**: Actions and their results can be tested without mocking
4. **Clear Dependencies**: Domain-specific handlers create explicit boundaries
5. **Composability**: Actions and effects can be composed and chained in flexible ways
6. **Explicit Side Effects**: All side effects are represented as data and executed in controlled handlers
7. **Developer Experience**: Debugging is simplified by logging actions and effects as data

This architecture provides a solid foundation for building the Calva MCP Server, leveraging ClojureScript's strengths in functional programming and data-oriented design.