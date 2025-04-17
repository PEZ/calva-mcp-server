(ns calva-mcp-server.stdio.wrapper
  (:require
   ["process" :as process]
   ["net" :as net]
   ["fs" :as fs]
   ["path" :as path]
   [promesa.core :as p]))

(def port-file-path (path/join ".calva" "mcp-server" "port"))

(defn read-port-from-file []
  (p/create
   (fn [resolve _reject]
     (.readFile fs port-file-path #js {:encoding "utf8"}
                (fn [err data]
                  (if err
                    (resolve nil)
                    (try
                      (let [port-num (js/parseInt data 10)]
                        (if (js/isNaN port-num)
                          (resolve nil)
                          (resolve port-num)))
                      (catch :default _e
                        (resolve nil)))))))))

(defn ^:export main [& _args]
  (p/let [port (read-port-from-file)]
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
               (js/console.log "Socket closed, exiting.")
               (.exit process 0)))

        (.on stdin "close"
             (fn []
               (js/console.log "Stdin closed, closing socket.")
               (.end socket)))

        (js/console.log (str "stdio wrapper connected to port " port)))

      (do
        (js/console.error (str "Port file not found or invalid: " port-file-path))
        (.write (.-stdout process)
                (str (js/JSON.stringify
                      #js {:jsonrpc "2.0",
                           :error #js {:code -32001,
                                       :message "Calva MCP backend server not running. Please start it using the 'Calva MCP Server: Start MCP Server' command in VS Code."}})
                     "\n"))
        (.exit process 1)))))
