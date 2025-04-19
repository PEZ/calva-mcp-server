(ns calva-mcp-server.extension.fxs
  (:require
   [calva-mcp-server.mcp.logging :as logging]
   [clojure.core.match :refer [match]]))

(defn log
  "Logs a message to the console and file.
   Takes a context map, level, and message."
  [context {:keys [level message]}]
  (let [log-dir-uri (:context/logUri context)
        config {:app/log-dir-uri log-dir-uri
                :app/min-log-level (get-in context [:ex/db :app/min-log-level] :debug)}]
    (case level
      :error (logging/error! config message)
      :warn (logging/warn! config message)
      :info (logging/info! config message)
      :debug (logging/debug! config message)
      (logging/debug! config message)))
  nil)

(defn perform-effect! [dispatch! context [effect-kw & args :as effect]]
  (match effect-kw
    :extension/fx.log (log context (first args))
    :else (js/console.warn "Unknown extension effect:" (pr-str effect-kw))))