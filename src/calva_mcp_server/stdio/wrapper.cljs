(ns calva-mcp-server.stdio.wrapper
  (:require
   ["process" :as process]
   ["net" :as net]))

(defn ^:export main [& args]
  (let [port (or (some-> (nth args 0 nil) parse-long) 9000)
        socket (net/connect #js {:port port})
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
           (.write stdout (str (js/JSON.stringify #js {:jsonrpc "2.0", :error #js {:code -32000, :message "Server error"}}) "\n"))
           (.exit process 1)))

    (.on socket "close"
         (fn []
           (js/console.log "Socket closed, exiting.")
           (.exit process 0)))

    (.on stdin "close"
         (fn []
           (js/console.log "Stdin closed, closing socket.")
           (.end socket)))

    (js/console.log (str "stdio wrapper connected to port " port))))
