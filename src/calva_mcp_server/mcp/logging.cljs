(ns calva-mcp-server.mcp.logging
  (:require ["vscode" :as vscode]
            ["fs" :as fs]
            [promesa.core :as p]))

(defn append-file+ [path data]
  (p/create
   (fn [resolve-fn reject]
     (fs/appendFile path data
                    (fn [err]
                      (if err
                        (reject err)
                        (resolve-fn)))))))

(defn get-log-path [log-dir-uri]
  (vscode/Uri.joinPath log-dir-uri "mcp-server.log"))

(defn log! [log-dir-uri level & messages]
  (let [timestamp (.toISOString (js/Date.))
        formatted-message (apply str timestamp " [" (name level) "] " (map pr-str messages))
        log-entry (str formatted-message "\n")]
    (-> (p/let [_ (vscode/workspace.fs.createDirectory log-dir-uri)
                ^js log-uri (get-log-path log-dir-uri)]
          (append-file+ (.-fsPath log-uri) log-entry))
        (p/catch (fn [err]
                   (js/console.error "Failed to write to MCP server log:" err))))))

(defn info! [log-dir-uri & messages]
  (apply js/console.log messages)
  (apply log! log-dir-uri :info messages))

(defn error! [log-dir-uri & messages]
  (apply js/console.error messages)
  (apply log! log-dir-uri :error messages))

(defn warn! [log-dir-uri & messages]
  (apply js/console.error messages)
  (apply log! log-dir-uri :warn messages))

(defn debug! [log-dir-uri & messages]
  (apply js/console.debug messages)
  (apply log! log-dir-uri :debug messages))