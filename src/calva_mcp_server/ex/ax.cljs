(ns calva-mcp-server.ex.ax
  (:require
   [calva-mcp-server.extension.axs :as extension-axs]
   [calva-mcp-server.hello.axs :as hello-axs]
   [calva-mcp-server.integrations.node.axs :as node-axs]
   [calva-mcp-server.integrations.vscode.axs :as vscode-axs]
   [calva-mcp-server.ex.test.axs :as ex-test-axs]
   [calva-mcp-server.mcp.axs :as mcp-axs]
   [clojure.core.match :refer [match]]
   [clojure.string :as string]
   [clojure.walk :as walk]))

(defn- js-get-in
  "Returns the value from the JavaScript `object` following the sequence of strings as a `path`.

   ```clojure
   (js-get-in #js {:a #js {:b 1}} [\"a\" \"b\"]) ;=> 1

   (def o (js/Object. (clj->js {:target {:value \"foo\"}})))
   (js-get-in o [\"target\" \"value\"]) ; => \"foo\"
   ```

   Does not throw an exception if the path does not exist, returns `nil` instead.
   ```clojure
   (js-get-in o [\"target\" \"value\" \"bar\"]) ; => nil
   (js-get-in o [\"TARGET\" \"value\"]) ; => nil
   ```"
  [object path]
  (reduce (fn [acc k]
            (some-> acc (unchecked-get k)))
          object
          path))

(defn enrich-action-from-context [action context]
  (walk/postwalk
   (fn [x]
     (if (keyword? x)
       (cond (= "context" (namespace x)) (let [path (string/split (name x) #"\.")]
                                           (js-get-in context path))
             :else x)
       x))
   action))

(defn- enrich-action-from-state [action state]
  (walk/postwalk
   (fn [x]
     (cond
       (and (vector? x) (= :db/get (first x)))
       (get state (second x))

       :else x))
   action))

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

       :else
       x))
   actions))

(defn handle-action [state context [action-kw :as action]]
  (let [enriched-action (-> action
                            (enrich-action-from-context context)
                            (enrich-action-from-state state))]
    (match (namespace action-kw)
      "hello"        (hello-axs/handle-action state context enriched-action)
      "vscode"       (vscode-axs/handle-action state context enriched-action)
      "node"         (node-axs/handle-action state context enriched-action)
      "ex-test"      (ex-test-axs/handle-action state context enriched-action)
      "mcp"          (mcp-axs/handle-action state context enriched-action)
      "extension"    (extension-axs/handle-action state context enriched-action)
      :else {:fxs [[:node/fx.log-error "Unknown action namespace for action:" (pr-str action)]]})))

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