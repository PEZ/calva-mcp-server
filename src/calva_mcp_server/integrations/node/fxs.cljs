(ns calva-mcp-server.integrations.node.fxs
  (:require [clojure.core.match :refer [match]]))

(defn perform-effect! [_dispatch! _context effect]
  (match effect
    [:node/fx.log & args]
    (apply js/console.log args)

    [:node/fx.log-error & args]
    (apply js/console.error args)

    :else
    (js/console.error "Unknown effect:" (pr-str effect))))