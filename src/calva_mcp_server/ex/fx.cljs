(ns calva-mcp-server.ex.fx
  (:require [clojure.core.match :refer [match]]))

(defn perform-effect! [event-handler ctx effect]
  (match effect
    ;; Hello world effect
    [:hello/fx.log-greeting greeting]
    (js/console.log greeting)

    ;; Default case for unknown effects
    :else
    (js/console.warn "Unknown effect:" (pr-str effect))))