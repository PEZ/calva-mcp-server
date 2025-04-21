(ns calva-mcp-server.app.db)

(def init-db {:extension/context nil
              :extension/disposables []
              :extension/when-contexts {:calva-mcp-extension/activated? false}})

(defonce !app-db (atom init-db))

(comment
  @!app-db
  :rcf)