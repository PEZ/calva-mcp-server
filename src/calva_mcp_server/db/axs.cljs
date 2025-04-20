(ns calva-mcp-server.db.axs
  (:require
   [cljs.core.match :refer [match]]))

(defn handle-action [state _context action]
  (match action
    [:db/ax.update-in path & args]
    {:ex/db (apply update-in state path args)}

    :else
    {:ex/fxs [[:node/fx.log-error "Unknown action:" (pr-str action)]]}))