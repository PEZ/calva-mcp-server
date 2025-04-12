(ns vsc-et.quick-pick
  (:require ["vscode" :as vscode]
            [vsc-et.extension.db :as db]
            [promesa.core :as p]))

(defn memoized-quick-pick!
  "Shows a vscode/quickPick menu with the given items and options.
   `quick-pick-options` is a Clojure map of options matching `vscode/QuickPickOptions`.
   The quickPick will remember and pre-select the last selected item(s)
   using the `save-as` key."
  [items quickpick-options save-as]
  (let [qp ^js (vscode/window.createQuickPick)
        context ^js (:extension/context @db/!app-db)
        ws-state ^js (.-workspaceState context)
        saved-label (.get ws-state save-as)
        saved-items (.filter items (fn [^js item]
                                     (= saved-label (.-label item))))]
    (doseq [option (keys quickpick-options)]
      (unchecked-set qp (name option) (quickpick-options option)))
    (unchecked-set qp "items" items)
    (unchecked-set qp "activeItems" saved-items)
    (p/create
     (fn [resolve _reject]
       (doto qp
         (.show)
         (.onDidAccept (fn []
                         (if (< 0 (-> qp .-selectedItems .-length))
                           (p/let [choice (-> qp .-selectedItems (unchecked-get 0))]
                             (resolve choice)
                             (.update ws-state save-as (.-label choice)))
                           (resolve js/undefined))
                         (.hide qp)))
         (.onDidHide (fn []
                       (resolve #js [])
                       (.hide qp))))))))