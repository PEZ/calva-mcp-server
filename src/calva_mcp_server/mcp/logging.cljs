(ns calva-mcp-server.mcp.logging
  (:require ["vscode" :as vscode]
            [promesa.core :as p]))

(defn- get-log-path []
  (when-let [ext-dir (.-logUri vscode/env)]
    (let [log-uri (.joinPath vscode/Uri (.toString ext-dir) "mcp-server.log")
          dir-uri (.joinPath vscode/Uri (.toString ext-dir) "")]
      (-> (p/do!
           (.createDirectory vscode/workspace.fs dir-uri)
           log-uri)))))

(defn log! [level message & data]
  (let [timestamp (.toISOString (js/Date.))
        formatted-message (str timestamp " [" (name level) "] " message
                               (when data (str " " (pr-str data))))
        log-entry (str formatted-message "\n")
        log-entry-data (js/Buffer.from log-entry)]
    (-> (p/let [log-uri (get-log-path)]
          (when log-uri
            (.writeFile vscode/workspace.fs log-uri log-entry-data #js {:create true, :overwrite false})))
        (p/catch (fn [err]
                   (js/console.error "Failed to write to MCP server log:" err))))))

(defn info! [& messages]
  (apply js/console.log messages)
  (apply log! :info messages))

(defn error! [& messages]
  (apply js/console.error messages)
  (apply log! :error messages))

(defn warn! [& messages]
  (apply js/console.error messages)
  (apply log! :warn messages))

(defn debug! [& messages]
  (apply js/console.debug messages)
  (apply log! :debug messages))