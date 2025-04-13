(ns calva-mcp-server.ex.event-handler
  (:require [calva-mcp-server.ex.ax :as ax]
            [calva-mcp-server.ex.fx :as fx]))

(defn event-handler [ctx actions]
  (let [{:ex/keys [fxs dxs db]} (ax/handle-actions {} ctx actions)]
    ;; Process effects
    (when fxs
      (doseq [effect fxs]
        (fx/perform-effect! event-handler ctx effect)))

    ;; Process dispatched actions - but only if there are any
    (when (seq dxs)
      (event-handler ctx dxs))

    ;; Return the updated state for use by callers
    db))

(defn create-event-handler [initial-state]
  (let [!state (atom initial-state)]
    (fn dispatch [ctx actions]
      (let [{:ex/keys [fxs dxs db]} (ax/handle-actions @!state ctx actions)]
        ;; Update state
        (when db
          (reset! !state db))

        ;; Process effects
        (when fxs
          (doseq [effect fxs]
            (fx/perform-effect! dispatch ctx effect)))

        ;; Process dispatched actions - but only if there are any
        (when (seq dxs)
          (dispatch ctx dxs))

        ;; Return the updated state for use by callers
        @!state))))