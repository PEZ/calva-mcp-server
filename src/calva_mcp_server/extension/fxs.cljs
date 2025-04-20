(ns calva-mcp-server.extension.fxs
  (:require
   [calva-mcp-server.mcp.logging :as logging]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [dispatch! _context effect]
  (match effect
    [:extension/fx.log options level & messages]
    (apply logging/log! (assoc options :ex/dispatch! dispatch!) level messages)

    :else
    (js/console.warn "Unknown extension effect:" (pr-str effect))))