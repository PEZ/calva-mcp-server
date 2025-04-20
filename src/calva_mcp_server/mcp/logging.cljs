(ns calva-mcp-server.mcp.logging
  (:require
   ["fs" :as fs]
   ["vscode" :as vscode]
   [promesa.core :as p]))

(defn append-file+ [path data]
  (p/create
   (fn [resolve-fn reject]
     (fs/appendFile path data
                    (fn [err]
                      (if err
                        (reject err)
                        (resolve-fn)))))))

(defn get-log-path [{:app/keys [log-uri]}]
  (vscode/Uri.joinPath log-uri "mcp-server.log"))

(defn log! [{:app/keys [log-uri] :as config} level & messages]
  (let [timestamp (.toISOString (js/Date.))
        formatted-message (apply str timestamp " [" (name level) "] " (map pr-str messages))
        log-entry (str formatted-message "\n")]
    (-> (p/let [_ (vscode/workspace.fs.createDirectory log-uri)
                ^js log-file-uri (get-log-path config)]
          (append-file+ (.-fsPath log-file-uri) log-entry))
        (p/catch (fn [err]
                   (js/console.error "Failed to write to MCP server log:" err))))))
