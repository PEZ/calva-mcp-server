(ns calva-mcp-server.mcp.logging
  (:require ["vscode" :as vscode]
            ["fs" :as fs]
            [promesa.core :as p]
            [clojure.string :as str]))

;; Define log levels with numeric values for comparison
(def log-levels {:error 0
                 :warn 1
                 :info 2
                 :debug 3})

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

(defn should-log? [config level]
  (let [min-level (get config :app/min-log-level :debug)]
    (<= (get log-levels level)
        (get log-levels min-level))))

(defn log! [{:app/keys [log-uri] :as config}level & messages]
  (when (should-log? config level)
    (let [timestamp (.toISOString (js/Date.))
          formatted-message (apply str timestamp " [" (name level) "] " (map pr-str messages))
          log-entry (str formatted-message "\n")]
      (-> (p/let [_ (vscode/workspace.fs.createDirectory log-uri)
                  ^js log-file-uri (get-log-path config)]
            (append-file+ (.-fsPath log-file-uri) log-entry))
          (p/catch (fn [err]
                     (js/console.error "Failed to write to MCP server log:" err)))))))

(defn info! [config & messages]
  (apply log! config :info messages))

(defn error! [config & messages]
  (apply log! config :error messages))

(defn warn! [config & messages]
  (apply log! config :warn messages))

(defn debug! [config & messages]
  (apply log! config :debug messages))