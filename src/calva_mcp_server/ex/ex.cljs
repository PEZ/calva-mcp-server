(ns calva-mcp-server.ex.ex
  (:require
   [calva-mcp-server.ex.ax :as ax]
   [calva-mcp-server.ex.fx :as fx]
   [calva-mcp-server.extension.db :as db]))

(defn dispatch! [extension-context actions]
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @db/!app-db extension-context actions)]
    (when db
      (reset! db/!app-db db))
    (when (seq dxs)
      (dispatch! extension-context dxs))
    (when (seq fxs)
      (doseq [fx fxs]
        #_(when js/goog.DEBUG (js/console.debug "Triggered effect" effect))
        (fx/perform-effect! dispatch! extension-context fx)))))