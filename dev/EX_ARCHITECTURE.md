# Calva Backseat Driver - Ex Architecture

**Ex** is a pico-framework (some 50 LOC or so) that implements a functional core/imperative shell design, emphasizing immutability, pure functions, and unidirectional data flow. Heavily inspired by [re-frame](https://github.com/day8/re-frame) (used in its most data-oriented way). It's also a lot how I found myself holding [Replicant](https://replicant.fun) to keep my re-frame habits.

(Appologies in advance for any marketing language used here. A lot of this description is written by Claude when I asked it to summarize the architecture it found in the calva-backseat-driver codebase.)

## Core Concepts

The Ex architecture revolves around these key concepts:

1. **Actions (`:ex/axs`)**: Pure data structures representing operations to perform, structured as vectors with a namespaced keyword identifier and parameters
2. **Effects (`:ex/fxs`)**: Data structures representing controlled side effects. The effects dispatcher will call the actual functions.
3. **Action Enrichment**: System for transforming pure data structures with contextual information at runtime
4. **Dispatch**: Mechanism for **actions** dispatch, available to all actions (`:ex/dxs`) and effects (they get passed a `dispatch!` function)
5. **Central Application State**: Actions get the current state as an immutable map and return new state. State is centrally updated on a the app-db atom in the dispatch handler. Basically it is treated as the side effect it is.

## Evolutionary Design

The Ex framework is embedded directly in the project source (in the `calva_mcp_server.ex` namespace) rather than imported as a library. This is intentional, allowing the framework to evolve alongside the project's needs. As requirements change, the framework can be extended with:

- New enrichments
- Additional effects
- Modified dispatch behaviors
- Anything

This evolutionary approach ensures the architecture remains flexible and tailored to the specific needs of the Calva Backseat Driver project.

## System Flow

The Ex architecture follows a unidirectional data flow pattern with clear separation between pure functions and side effects:

1. `dispatch!` function receives an actions collection and calls `handle-actions`
2. `handle-actions` processes each action (`ax`) sequentially with the latest state
3. Each individual action goes through:
   - Enrichment (from extension context and app state)
   - Domain-specific handling
   - Result generation a map with any (or none) of the `:ex/db`, `:ex/fxs`, `:ex/dxs` entries.
4. Results are accumulated into a single batch result
5. The dispatcher then:
   - Updates application state via `reset!` (a controlled side effect)
   - Processes any resulting dispatched actions (`:ex/dxs`)
   - Executes effects (`:ex/fxs`)

## Core Components

### 1. Action Handler (ax.cljs)

The action handler processes actions by:
1. Enriching actions with context and state
2. Routing actions to domain-specific handlers based on namespace
3. Returning action results with new state and effects

```clojure
(defn handle-action [state context [action-kw :as action]]
  (let [enriched-action (-> action
                            (enrich-action-from-context context)
                            (enrich-action-from-state state))]
    (match (namespace action-kw)
      "vscode"    (vscode-axs/handle-action state context enriched-action)
      "node"      (node-axs/handle-action state context enriched-action)
      "mcp"       (node-axs/handle-action state context enriched-action)
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

Supports passing arguments from effects to subsequent actions:

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

For nested Async Flows a pattern used throughout the framework is the `:ex/then` option for effects that return promises:

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

For other types of effects similar patterns are used, such as `:ex/on-success`, `:ex/on-failure`. (You may find it better to stick to just one pattern.)

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

1. **vscode**: VS Code-specific operations
1. **node**: Node.js operations like logging
1. **mcp**: the mcp server
1. **test**: used by the unit tests for testing Ex itself

Each domain has its own action and effect handlers, following a consistent pattern.

## Async Operations

The architecture supports async operations primarily through effect handlers. For example, VS Code input boxes return Promises, which are then used to trigger follow-up actions:

```clojure
[:vscode/fx.show-input-box {:title "Hello Input"
                           :ex/then [[:hello/ax.say-hello :ex/action-args]]}]
```

This creates a clean way to handle asynchronous flows while maintaining pure data structures for actions and effects.

## Cons

* Deep event chains can sometimes be a bit hard to orient yourself in
* The indirection between actually performing a side effect and declaring it is an inderection after all
* While a subdomain of side effects is being implemented (as needed, mind you), it can be a bit of extra work to get it in place. Basically you'll have at least one interface declaration for each side effect you use

## Pros

1. **Functional and Data Oriented**: Business logic is implemented in pure functions that transform data
1. **Improved Testability**: Actions and their results can be tested without mocking
1. **Predictable Dataflow**: All state inside actions is immutable; new state is created rather than mutating existing state
1. **Clear Dependencies**: Domain-specific handlers create explicit boundaries
1. **Composability**: Actions and effects can be composed and chained in flexible ways
1. **Explicit Side Effects**: All side effects are represented as data and executed in controlled handlers
1. **Developer Experience**: Debugging is simplified by logging actions and effects as data

I believe that the benefits outweigh the drawbacks, and that this architecture provides a solid foundation for building the Calva Backseat Driver, leveraging ClojureScript's strengths in functional programming and data-oriented design.