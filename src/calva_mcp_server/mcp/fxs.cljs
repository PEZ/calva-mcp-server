(ns calva-mcp-server.mcp.fxs
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.mcp.server :as server]
   [cljs.core.match :refer [match]]
   [promesa.core :as p]))

(defn perform-effect! [dispatch! ^js context effect]
  (match effect
    [:mcp/fx.start-server options]
    (let [{:ex/keys [on-success on-error]} options]
      (-> (server/start-server!+ (assoc options :ex/dispatch!
                                        (partial dispatch! context)))
          (p/then (fn [server-info]
                    (dispatch! context (ax/enrich-with-args on-success server-info))))
          (p/catch
           (fn [e]
             (dispatch! context (ax/enrich-with-args on-error e))))))

    [:mcp/fx.stop-server options]
    (let [{:ex/keys [on-success on-error]} options]
      (-> (p/catch
           (server/stop-server!+ (assoc options :ex/dispatch!
                                        (partial dispatch! context)))
           (fn [e]
             (dispatch! context (ax/enrich-with-args on-error (.-message e)))))
          (p/then (fn [_]
                    (dispatch! context on-success)))))

    [:mcp/fx.send-notification-params params]
    (server/send-notification-params {:ex/dispatch! (partial dispatch! context)} params)

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))