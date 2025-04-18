(ns calva-mcp-server.mcp.server
  (:require
   ["net" :as net]
   ["vscode" :as vscode]
   [calva-mcp-server.mcp.logging :as logging]
   [clojure.string :as str]
   [promesa.core :as p]))

(defn- get-workspace-root-uri []
  (some-> vscode/workspace.workspaceFolders
          first
          .-uri))

(defn- get-server-dir []
  (vscode/Uri.joinPath (get-workspace-root-uri) ".calva" "mcp-server"))

(defn- get-port-file-uri []
  (vscode/Uri.joinPath (get-server-dir) "port"))

(defn- ensure-port-file-dir-exists []
  (vscode/workspace.fs.createDirectory (get-server-dir)))

(def ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^js calvaApi (-> calvaExt
                      .-exports
                      .-v1
                      (js->clj :keywordize-keys true)))

(defn evaluate-code [code session]
  (p/let [evaluation+ ((get-in calvaApi [:repl :evaluateCode]) session code)
          result (.-result evaluation+)]
    result))

(comment
  (evaluate-code "(+ 41 1)" js/undefined)
  :rcf)

(def ^:private tools [{:name "calva-eval"
                       :description "Evaluate Clojure/ClojureScript code"
                       :inputSchema {:type "object"
                                     :properties {"code" {:type "string"
                                                         :description "Clojure/ClojureScript code to evaluate"}}
                                     :required ["code"]}}])

(defn handle-request-fn [log-uri {:keys [id method params] :as request}]
  (logging/debug! log-uri "BOOM! handle-request " (pr-str request))
  (cond
    (= method "initialize")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:serverInfo {:name "calva-mcp-server"
                                          :version "0.0.1"}
                             :protocolVersion "2024-11-05"
                             :capabilities {:tools {:listChanged true}}
                             :instructions "Use the calva-eval tool to evaluate Clojure/ClojureScript code in the current project."}}]
      response)

    (= method "tools/list")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:tools tools}}]
      (logging/debug! log-uri "Formatted tools response:" (pr-str tools))
      response)

    (= method "tools/call")
    (let [{:keys [arguments]
           tool :name} params]
      (p/let [result (if (= tool "calva-eval")
                       (evaluate-code (:code arguments) js/undefined)
                       nil)]
        (if result
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                               :text (str result)}]}}
          {:jsonrpc "2.0"
           :id id
           :error {:code -32601
                   :message "Unknown tool"}})))

    (= method "ping")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {}}]
      response)

    id
    {:jsonrpc "2.0" :id id :error {:code -32601 :message "Method not found"}}

    :else ;; returning nil so that the response is not sent
    nil))

(defn create-request-handler [log-uri]
  (fn [request]
    (handle-request-fn log-uri request)))

(defn start-socket-server [log-uri]
  (let [handle-request (create-request-handler log-uri)]
    (p/create
     (fn [resolve-fn reject]
       (try
         (let [server (.createServer
                       net
                       (fn [^js socket]
                         (.setEncoding socket "utf8")
                         (let [buffer (volatile! "")] ; Buffer for incoming socket data
                           (.on socket "data"
                                (fn [a-chunk]
                                  (logging/debug! log-uri "[Server] Socket received chunk:" a-chunk)
                                  (vswap! buffer str a-chunk)
                                  ;; Process buffer, splitting by newline
                                  (loop []
                                    (let [buffer-val @buffer
                                          newline-pos (.indexOf buffer-val "\n")]
                                      (if (>= newline-pos 0) ; Found a newline
                                        (let [message-part (subs buffer-val 0 newline-pos)
                                              ;; Update buffer *before* processing message
                                              _ (vreset! buffer (subs buffer-val (inc newline-pos)))
                                              request-json (str/trim message-part)] ; Use str/trim
                                          (if (not (str/blank? request-json)) ; Use str/blank?
                                            (try
                                              (logging/debug! log-uri "[Server] Processing request segment:" request-json)
                                              ;; Wrap the processing in a p/do! or similar if handle-request is async
                                              ;; and might throw errors that need catching by the outer try/catch.
                                              ;; Using p/let here is fine as long as handle-request returns a promise
                                              ;; or a plain value.
                                              (p/let [request-js (js/JSON.parse request-json) ; Parse *only* the segment
                                                      {:keys [method] :as request} (js->clj request-js :keywordize-keys true)
                                                      _ (logging/debug! log-uri "[Server] Parsed request:" (pr-str request))
                                                      response (handle-request request)]
                                                (if response
                                                  (do
                                                    (logging/debug! log-uri "[Server] Sending response for" method ":" (pr-str response))
                                                    (.write socket (str (js/JSON.stringify (clj->js response)) "\n")))
                                                  (logging/debug! log-uri "[Server] No response generated for method:" method))
                                                 ;; No need to reset buffer here, it was updated before parsing
                                                )
                                              (catch js/Error parse-err ; Catch JS errors specifically if needed
                                                (logging/error! log-uri "[Server] Error parsing request JSON segment:" (.-message parse-err) {:json request-json})
                                                (.write socket (str (js/JSON.stringify #js {:jsonrpc "2.0", :error #js {:code -32700, :message "Parse error"}}) "\n"))
                                                ;; No need to reset buffer here either
                                                ))
                                            (logging/debug! log-uri "[Server] Blank line segment received, ignoring."))
                                          (recur)) ; Check buffer again for more complete messages
                                        false))))) ; No more newlines in the buffer, wait for more data
                           (.on socket "error" (fn [err]
                                                 (logging/error! log-uri "[Server] Socket error:" err))))))]
           (.on server "error" (fn [err]
                                 (logging/error! log-uri "[Server] Server creation error:" err)
                                 (reject err)))
           (.listen server 0 (fn []
                               (let [address (.address server)
                                     port (.-port address)]
                                 (logging/info! log-uri "[Server] Socket server listening on port" port)
                                 (resolve-fn {:server/instance server :server/port port})))))
         (catch js/Error e ; Catch JS errors specifically
           (logging/error! log-uri "[Server] Error creating server:" (.-message e))
           (reject e)))))))

(defn start-server [{:app/keys [log-uri]}]
  (p/let [server-info (start-socket-server log-uri)
          port (:server/port server-info)
          ^js port-file-uri (get-port-file-uri)]
    (if port-file-uri
      (p/do!
       (ensure-port-file-dir-exists)
       (.writeFile vscode/workspace.fs port-file-uri (js/Buffer.from (str port)))
       (logging/info! log-uri "Wrote port file:" (.-fsPath port-file-uri))
       (assoc server-info :server/log-uri (logging/get-log-path log-uri)))
      (do
        (logging/error! log-uri "Could not determine workspace root to write port file.")
        (p/rejected (js/Error. "Could not determine workspace root"))))))

(defn stop-server [{:keys [app/log-uri server/instance]}]
  (if instance
    (p/let [^js port-file-uri (get-port-file-uri)
            _ (logging/info! log-uri "Stopping socket server...")]
      (-> (p/create (fn [resolve-fn reject]
                      (.close instance (fn [err]
                                         (if err
                                           (do
                                             (logging/error! log-uri "Error stopping socket server:" err)
                                             (reject err))
                                           (do
                                             (logging/info! log-uri "Socket server stopped.")
                                             (resolve-fn true)))))))
          (p/then (fn [_]
                    (if port-file-uri
                      (-> (vscode/workspace.fs.delete port-file-uri #js {:recursive false, :useTrash false})
                          (p/then (fn [_] (logging/info! log-uri "Deleted port file:" (.fsPath port-file-uri)) true))
                          (p/catch (fn [_del-err]
                                     (logging/warn! log-uri "Could not delete port file (maybe already gone?).")
                                     true)))
                      (p/resolved true))))
          (p/catch (fn [err]
                     (logging/error! log-uri "Error during server stop or file deletion:" err)
                     (p/rejected err)))))
    (do
      (logging/info! log-uri "No server instance provided to stop.")
      (p/resolved false))))

(comment
  ;; Todo fix working RCF
  ;; (start-server)
  ;; (stop-server)
  :rcf)

