(ns calva-mcp-server.stdio.wrapper
  (:require
   ["fs" :as fs]
   ["net" :as net]
   ["process" :as process]
   [clojure.string :as string]
   [promesa.core :as p]))

(def log-levels {:error 0
                 :warn 1
                 :info 2
                 :debug 3})

(def min-log-level
  (let [arg-level (some #(when (.startsWith % "--min-log-level=")
                           (subs % (count "--min-log-level=")))
                        (js->clj (.-argv process)))
        level-kw (when arg-level (keyword arg-level))]
    (get log-levels level-kw :debug)))

(defn log-stderr
  ([args] (log-stderr :debug args))
  ([level & args]
   (when (>= (get log-levels :debug) (get log-levels level min-log-level))
     (.write (.-stderr process) (str "[Wrapper] " (string/join " " args) "\n")))))

;; Redirect console output to stderr, defaulting to debug level
(set! js/console.log (partial log-stderr :debug))
(set! js/console.error (partial log-stderr :error))

(def original-stdout (.-stdout process))

(defn read-port-from-file [port-file-path]
  (p/create
   (fn [resolve-fn _reject]
     (.readFile fs port-file-path #js {:encoding "utf8"}
                (fn [err data]
                  (if err
                    (do (log-stderr :error "Port file read error:" err)
                        (resolve-fn nil))
                    (let [port-num (js/parseInt data 10)]
                      (if (js/isNaN port-num)
                        (do (log-stderr :error "Invalid port number in file:" data)
                            (resolve-fn nil))
                        (resolve-fn port-num)))))))))

(defn handle-stdin [^js stdin ^js socket]
  (let [stdin-buffer (volatile! "")]
    (.setEncoding stdin "utf8")

    ;; Handle stdin data
    (.on stdin "data"
         (fn [chunk]
           (log-stderr :debug "Raw stdin chunk received:" chunk)
           (vswap! stdin-buffer str chunk)
           ;; Process buffer, splitting by newline
           (loop []
             (let [buffer-val @stdin-buffer
                   newline-pos (.indexOf buffer-val "\n")]
               (if (>= newline-pos 0) ; Found a newline
                 (let [message-part (subs buffer-val 0 newline-pos)
                       ;; Update buffer *before* processing message
                       _ (vreset! stdin-buffer (subs buffer-val (inc newline-pos)))
                       message (string/trim message-part)]
                   (if (not (string/blank? message))
                     (do
                       (log-stderr :info "Complete message segment from stdin, sending to socket:" message)
                       (.write socket (str message "\n"))) ; Send message + newline
                     (log-stderr :warn "Blank line segment received, ignoring."))
                   (recur)) ; Check buffer again for more complete messages
                 false))))) ; No more newlines in the buffer, wait for more data

    ;; Handle stdin errors
    (.on stdin "error" (fn [err] (log-stderr :error "stdin error:" err)))

    ;; Handle stdin close
    (.on stdin "close" (fn [] (log-stderr :info "stdin closed.")))))

(defn handle-socket [^js socket]
  (.setEncoding socket "utf8")

  ;; Forward socket server responses to stdout
  (.on socket "data"
       (fn [data]
         (log-stderr :debug "Received from socket:" data)
         (let [message-str (string/trim data)]
           (if (and (string? message-str) (not (string/blank? message-str))
                    (or (.startsWith message-str "{") (.startsWith message-str "[")))
             (do
               (log-stderr :info "Sending to stdout:" message-str)
               (.write original-stdout (str message-str "\n")))
             (log-stderr :warn "Filtered potential non-JSON output from socket:" message-str)))))

  ;; Handle socket errors
  (.on socket "error"
       (fn [err]
         (log-stderr :error "Socket error:" err)
         (.write original-stdout
                 (str (js/JSON.stringify
                       #js {:jsonrpc "2.0"
                            :error #js {:code -32000
                                        :message (str "Server connection error: "
                                                      (.-message err))}})
                      "\n"))
         (.exit process 1)))

  ;; Handle socket close
  (.on socket "close"
       (fn [had-error?]
         (log-stderr :info (if had-error?
                             "Socket closed due to transmission error."
                             "Socket connection closed cleanly."))
         (.exit process (if had-error? 1 0)))))

(defn ^:export main [& args]
  (let [port-file-path (first args)]
    (if-not port-file-path
      (do
        (log-stderr :error "Error: Port file path argument missing.")
        (.write original-stdout
                (str (js/JSON.stringify
                      #js {:jsonrpc "2.0"
                           :error #js {:code -32002
                                       :message "Configuration error: Port file path not provided."}})
                     "\n"))
        (.exit process 1))
      (p/let [port (read-port-from-file port-file-path)]
        (if port
          (let [socket (net/connect #js {:port port})
                stdin (.-stdin process)]
            (handle-stdin stdin socket)
            (handle-socket socket)
            (log-stderr :info "Connected to MCP server on port" port))
          (do
            (log-stderr :error "Error: Port file not found:" port-file-path)
            (.write original-stdout
                    (str (js/JSON.stringify
                          #js {:jsonrpc "2.0"
                               :error #js {:code -32001
                                           :message "MCP server not running or port file missing."}})
                         "\n"))
            (.exit process 1)))))))