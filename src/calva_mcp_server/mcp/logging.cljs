(ns calva-mcp-server.mcp.logging
  (:require
   ["fs" :as fs]
   ["vscode" :as vscode]
   [promesa.core :as p]
   [clojure.string :as string]))

(defn init!+ [{:app/keys [log-file-uri]}]
  (-> (vscode/workspace.fs.createDirectory (vscode/Uri.joinPath log-file-uri ".."))
      (p/catch (fn [err]
                 (js/console.error "logging/init+ failed creating log file:" err)))))

(defn append-file+ [path data]
  (p/create
   (fn [resolve-fn reject]
     (fs/appendFile path data
                    (fn [err]
                      (if err
                        (reject err)
                        (resolve-fn)))))))

(defn log! [{:app/keys [^js log-file-uri]} level & messages]
  (let [timestamp (.toISOString (js/Date.))
        formatted-message (str timestamp " [" (name level) "] "
                               (string/join " " (map pr-str messages)))
        log-entry (str formatted-message "\n")]
    (-> (p/do!
         (append-file+ (.-fsPath log-file-uri) log-entry))
        (p/catch (fn [err]
                   (js/console.error "Failed to write to MCP server log:" err))))))
