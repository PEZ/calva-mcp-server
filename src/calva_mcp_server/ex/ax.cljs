(ns calva-mcp-server.ex.ax
  (:require [clojure.walk :as walk]
            [clojure.core.match :refer [match]]
            [calva-mcp-server.hello.axs :as hello-axs]
            [calva-mcp-server.node.axs :as node-axs]
            [calva-mcp-server.vscode.axs :as vscode-axs]))

(defn- enrich-action-from-context [action ctx]
  (walk/postwalk
   (fn [x]
     (cond
       (and (keyword? x) (= "ctx" (namespace x)))
       (get ctx (keyword (name x)))

       :else x))
   action))

(defn- enrich-action-from-state [action state]
  (walk/postwalk
   (fn [x]
     (cond
       (and (vector? x) (= :db/get (first x)))
       (get state (second x))

       :else x))
   action))

(defn handle-action [state ctx [action-kw :as action]]
  (let [enriched-action (-> action
                            (enrich-action-from-context ctx)
                            (enrich-action-from-state state))]
    (match (namespace action-kw)
      "hello"  (hello-axs/handle-action state ctx enriched-action)
      "vscode" (vscode-axs/handle-action state ctx enriched-action)
      "node"   (node-axs/handle-action state ctx enriched-action)
      :else {:fxs [[:node/fx.log-error "Unknown action namespace for action:" action]]})))

(defn handle-actions [state ctx actions]
  (reduce (fn [{state :ex/db :as acc} action]
            (let [{:ex/keys [db fxs dxs]} (handle-action state ctx action)]
              (cond-> acc
                db (assoc :ex/db db)
                dxs (update :ex/dxs into dxs)
                fxs (update :ex/fxs into fxs))))
          {:ex/db state
           :ex/fxs []
           :ex/dxs []}
          (remove nil? actions)))