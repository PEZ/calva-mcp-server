(ns calva-mcp-server.stdio.wrapper
  (:require
   ["fs" :as fs]
   ["net" :as net]
   ["path" :as path]
   ["process" :as process]
   [clojure.string :as string]
   [promesa.core :as p]))

;; Redirect all console.log output to stderr to avoid protocol pollution
(set! js/console.log (fn [& args]
                       (.write (.-stderr process) (str (apply str args) "\n"))))

;; Keep original stdout for JSON-RPC communication
(def original-stdout (.-stdout process))

(defn read-port-from-file [workspace-path]
  (let [port-file-path (path/join workspace-path ".calva" "mcp-server" "port")]
    (p/create
     (fn [resolve-fn _reject]
       (.readFile fs port-file-path #js {:encoding "utf8"}
                  (fn [err data]
                    (if err
                      (resolve-fn nil)
                      (let [port-num (js/parseInt data 10)]
                        (if (js/isNaN port-num)
                          (resolve-fn nil)
                          (resolve-fn port-num))))))))))

(defn ^:export main [& args]
  (let [workspace-path (first args)]
    (if-not workspace-path
      (do
        (js/console.error "Error: Workspace path argument missing.")
        (.write original-stdout
                (str (js/JSON.stringify
                      #js {:jsonrpc "2.0"
                           :error #js {:code -32002
                                       :message "Configuration error: Workspace path not provided."}})
                     "\n"))
        (.exit process 1))
      (p/let [port (read-port-from-file workspace-path)]
        (if port
          (let [socket (net/connect #js {:port port})
                stdin (.-stdin process)
                buffer (volatile! "")]

            (.setEncoding stdin "utf8")
            (.setEncoding socket "utf8")

            ;; Handle stdin from VS Code
            (.on stdin "data"
                 (fn [chunk]
                   (vswap! buffer str chunk)
                   (when (.endsWith @buffer "\n")
                     (let [message (string/trim @buffer)]
                       (.write socket (str message "\n")))
                     (vreset! buffer ""))))

            ;; Forward socket server responses to stdout
            (.on socket "data"
                 (fn [data]
                   ;; Filter out non-JSON-RPC output to prevent protocol breakage
                   (if (and (string? data)
                            (or (.startsWith data "{\"jsonrpc\":")
                                (.startsWith data "[{\"jsonrpc\":")))
                     (.write original-stdout data)
                     (js/console.error "Filtered socket output (sent to stderr):" data))))

            ;; Error handling
            (.on socket "error"
                 (fn [err]
                   (js/console.error "Socket error:" err)
                   (.write original-stdout (str (js/JSON.stringify
                                                 #js {:jsonrpc "2.0"
                                                      :error #js {:code -32000
                                                                  :message "Server connection error"}})
                                                "\n"))
                   (.exit process 1)))

            (.on socket "close"
                 (fn []
                   (.exit process 0)))

            (js/console.error (str "stdio wrapper connected to port " port)))
          (do
            (js/console.error (str "Port file not found or invalid at: " workspace-path))
            (.write original-stdout
                    (str (js/JSON.stringify
                          #js {:jsonrpc "2.0"
                               :error #js {:code -32001
                                           :message "MCP server not running or port file missing."}})
                         "\n"))
            (.exit process 1)))))))
