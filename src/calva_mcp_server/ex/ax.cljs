(ns calva-mcp-server.ex.ax
  (:require [clojure.walk :as walk]
            [clojure.core.match :refer [match]]))

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

(defn handle-action [state ctx action]
  (let [enriched-action (-> action
                            (enrich-action-from-context ctx)
                            (enrich-action-from-state state))]
    (match enriched-action
      ;; Hello world action
      [:hello/ax.say-hello name]
      {:ex/db (assoc state :hello/last-greeting (str "Hello, " name "!"))
       :ex/fxs [[:hello/fx.log-greeting (str "Hello, " name "!")]]
       :ex/dxs [[:hello/ax.greeting-sent]]}

      ;; After greeting sent action
      [:hello/ax.greeting-sent]
      {:ex/db (assoc state :hello/greeting-sent? true)}

      ;; Default case for unknown actions
      :else
      {:ex/db state})))

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