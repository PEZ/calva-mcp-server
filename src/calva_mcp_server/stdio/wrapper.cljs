(ns calva-mcp-server.stdio.wrapper
  (:require
   ["process" :as process]
   ["net" :as net]
   ["fs" :as fs]
   ["path" :as path]
   [promesa.core :as p]))

(defn read-port-from-file [workspace-path]
  (let [port-file-path (path/join workspace-path ".calva" "mcp-server" "port")]
    (p/create
     (fn [resolve _reject]
       (.readFile fs port-file-path #js {:encoding "utf8"}
                  (fn [err data]
                    (if err
                      (do
                        (js/console.error (str "Error reading port file: " port-file-path) err)
                        (resolve nil))
                      (try
                        (let [port-num (js/parseInt data 10)]
                          (if (js/isNaN port-num)
                            (do
                              (js/console.error (str "Invalid content in port file: " port-file-path) data)
                              (resolve nil))
                            (resolve port-num)))
                        (catch :default e
                          (js/console.error (str "Error parsing port file: " port-file-path) e)
                          (resolve nil))))))))))

(defn ^:export main [& args]
  (let [workspace-path (first args)]
    (if-not workspace-path
      (do
        (js/console.error "Error: Workspace path argument missing.")
        (.write (.-stdout process)
                (str (js/JSON.stringify
                      #js {:jsonrpc "2.0",
                           :error #js {:code -32002,
                                       :message "Configuration error: Workspace path not provided to MCP wrapper script."}})
                     "\n"))
        (.exit process 1))
      (p/let [port (read-port-from-file workspace-path)]
        (if port
          (let [socket (net/connect #js {:port port})
                stdin (.-stdin process)
                stdout (.-stdout process)
                buffer (volatile! "")]

            (.setEncoding stdin "utf8")

            (.on stdin "data"
                 (fn [chunk]
                   (vswap! buffer str chunk)
                   (when (.endsWith @buffer "\n")
                     (.write socket @buffer)
                     (vreset! buffer ""))))

            (.on socket "data"
                 (fn [data]
                   (.write stdout data)))

            (.on socket "error"
                 (fn [err]
                   (js/console.error "Socket error:" err)
                   (.write stdout (str (js/JSON.stringify #js {:jsonrpc "2.0", :error #js {:code -32000, :message "Server connection error"}}) "\n"))
                   (.exit process 1)))

            (.on socket "close"
                 (fn []
                   (js/console.error "Socket closed, exiting.")
                   (.exit process 0)))

            (.on stdin "close"
                 (fn []
                   (js/console.error "Stdin closed, closing socket.")
                   (.end socket)))

            (js/console.error (str "stdio wrapper connected to port " port)))

          (do
            (js/console.error (str "Port file not found or invalid relative to workspace: " workspace-path))
            (.write (.-stdout process)
                    (str (js/JSON.stringify
                          #js {:jsonrpc "2.0",
                               :error #js {:code -32001,
                                           :message "Calva MCP backend server not running or port file missing. Please start it using the 'Calva MCP Server: Start MCP Server' command in VS Code."}})
                         "\n"))
            (.exit process 1)))))))
