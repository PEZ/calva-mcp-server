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

(defn log-message [level message & [data]]
  (let [timestamp (.toISOString (js/Date.))
        formatted-message (str timestamp " [" (name level) "] " message
                               (when data (str " " (pr-str data))))
        log-entry (str formatted-message "\n")
        log-entry-data (js/Buffer.from log-entry)]
    (-> (p/let [log-uri (get-log-path)]
          (when log-uri
            (let [options #js {:create true, :overwrite false}]
              (.writeFile vscode/workspace.fs log-uri log-entry-data options))))
        (.catch (fn [err]
                  (js/console.error "Failed to write to MCP server log:" err))))))

(defn info [message & [data]]
  (js/console.log message (when data (pr-str data)))
  (log-message :info message data))

(defn error [message & [data]]
  (js/console.error message (when data (pr-str data)))
  (log-message :error message data))

(defn debug [message & [data]]
  (when (= "true" (.-MCP_SERVER_DEBUG js/process.env))
    (js/console.debug message (when data (pr-str data)))
    (log-message :debug message data)))