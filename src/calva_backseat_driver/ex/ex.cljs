(ns calva-backseat-driver.ex.ex
  (:require
   [calva-backseat-driver.ex.ax :as ax]
   [calva-backseat-driver.ex.fx :as fx]
   [calva-backseat-driver.app.db :as db]))

(defn dispatch! [extension-context actions]
  (when js/goog.DEBUG (js/console.debug "Ex dispatch!" (pr-str actions)))
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @db/!app-db extension-context actions)]
    (when db
      (reset! db/!app-db db))
    (when (seq dxs)
      (dispatch! extension-context dxs))
    (when (seq fxs)
      (last (map (fn [fx]
                   (when js/goog.DEBUG (js/console.debug "Ex Triggered effect" (pr-str fx)))
                   (fx/perform-effect! dispatch! extension-context fx))
                 fxs)))))