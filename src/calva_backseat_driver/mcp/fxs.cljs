(ns calva-backseat-driver.mcp.fxs
  (:require
   ["vscode" :as vscode]
   [calva-backseat-driver.ex.ax :as ax]
   [calva-backseat-driver.mcp.requests :as requests]
   [calva-backseat-driver.mcp.server :as server]
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
          (p/then (fn [success?]
                    (dispatch! context (ax/enrich-with-args on-success success?))))))

    [:mcp/fx.show-server-started-message server-info]
    (let [{:server/keys [port ^js port-file-uri]} server-info]
      (p/let [button (vscode/window.showInformationMessage (str "MCP socket server started on port: " port ". You also need to start the `calva` stdio server. (Check the docs of your AI Agent for how to do this.)") "Copy stdio-command")]
        (when (= "Copy stdio-command" button)
          (let [extension-uri (-> (vscode/extensions.getExtension
                                    "betterthantomorrow.calva-backseat-driver")
                                   .-extensionUri)
                script-uri (vscode/Uri.joinPath extension-uri "dist" "calva-mcp-server.js")
                script-path (.-fsPath script-uri)
                port-file-path (.-fsPath port-file-uri)]
            (vscode/env.clipboard.writeText
             (str "node " script-path " " port-file-path))))))

    [:mcp/fx.send-notification notification]
    (server/send-notification-params {:ex/dispatch! (partial dispatch! context)} notification)

    [:mcp/fx.handle-request options request]
    (requests/handle-request-fn (assoc options :ex/dispatch! (partial dispatch! context)) request)

    :else
    (js/console.warn "Unknown MCP effect:" (pr-str effect))))