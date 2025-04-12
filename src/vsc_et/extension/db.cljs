(ns vsc-et.extension.db)

(def init-db {:extension/context nil
              :extension/disposables []
              :extension/when-contexts {:vsc-et/active? false}})

(defonce !app-db (atom init-db))

(comment
  @!app-db
  :rcf)

