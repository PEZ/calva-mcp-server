(ns calva-mcp-server.mcp.fxs
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.mcp.server :as server]
   [cljs.core.match :refer [match]]
   [promesa.core :as p]))

(defn perform-effect! [dispatch! ^js context effect]
  (match effect
    [:mcp/fx.start-server options]
    (let [{:ex/keys [on-success on-error]
           :app/keys [log-uri]} options]
      (if log-uri
        (-> (server/start-server!+ (assoc options :ex/dispatch! dispatch!))
            (p/then (fn [server-info]
                      (dispatch! context (ax/enrich-with-args on-success server-info))))
            (p/catch
             (fn [e]
               (dispatch! context (ax/enrich-with-args on-error e)))))
        (dispatch! context (ax/enrich-with-args on-error (js/Error. "Log directory URI is not available")))))

    [:mcp/fx.stop-server options]
    (let [{:ex/keys [on-success on-error]} options]
      (-> (p/catch
           (server/stop-server!+ (assoc options :ex/dispatch! dispatch!))
           (fn [e]
             (dispatch! context (ax/enrich-with-args on-error (.-message e)))))
          (p/then (fn [_]
                    (dispatch! context on-success)))))

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))