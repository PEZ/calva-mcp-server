(ns calva-mcp-server.ex.ex
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.ex.fx :as fx]
   [calva-mcp-server.app.db :as db]))

(defn dispatch! [extension-context actions]
  (when js/goog.DEBUG (js/console.debug "Ex dispatch!" (pr-str actions)))
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @db/!app-db extension-context actions)]
    (when db
      (reset! db/!app-db db))
    (when (seq dxs)
      (dispatch! extension-context dxs))
    (when (seq fxs)
      (last (map (fn [fx]
                   (when js/goog.DEBUG (js/console.debug "Ex Triggered effect" fx))
                   (fx/perform-effect! dispatch! extension-context fx))
                 fxs)))))