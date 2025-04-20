(ns calva-mcp-server.extension.fxs
  (:require
   [calva-mcp-server.mcp.logging :as logging]
   [clojure.core.match :refer [match]]))

(defn perform-effect! [dispatch! _context effect]
  (match effect
    [:extension/fx.init-logging options]
    (logging/init!+ options)

    [:extension/fx.log options level messages]
    (let [min-level (:app/min-log-level options :debug)
          levels {:error 0, :warn 1, :info 2, :debug 3}]
      (when (<= (levels level) (levels min-level))
        (apply logging/log! (assoc options :ex/dispatch! dispatch!) level messages)))

    :else
    (js/console.warn "Unknown extension effect:" (pr-str effect))))