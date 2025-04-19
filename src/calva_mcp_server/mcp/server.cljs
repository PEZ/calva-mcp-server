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

(defn- ensure-port-file-dir-exists!+ []
  (vscode/workspace.fs.createDirectory (get-server-dir)))


(defn- delete-port-file!+ [config ^js port-file-uri]
  (p/create
   (fn [resolve-fn _reject]
     (if-not port-file-uri
       (resolve-fn true)
       (-> (vscode/workspace.fs.delete port-file-uri #js {:recursive false, :useTrash false})
           (p/then (fn [_]
                     (logging/info! config "Deleted port file:" (.-fsPath port-file-uri))
                     (resolve-fn true)))
           (p/catch (fn [err]
                      (logging/warn! config "Could not delete port file (maybe already gone?):" err)
                      (resolve-fn true))))))))
(def ^:private ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private ^js calvaApi (-> calvaExt
                                .-exports
                                .-v1
                                (js->clj :keywordize-keys true)))

(defn- evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and an optional REPL session."
  [code session]
  (p/let [evaluation+ ((get-in calvaApi [:repl :evaluateCode]) session code)
          result (.-result evaluation+)]
    result))

(comment
  (evaluate-code+ "(+ 41 1)" js/undefined)
  :rcf)

(def ^:private tools [{:name "calva-eval"
                       :description "Evaluate Clojure/ClojureScript code"
                       :inputSchema {:type "object"
                                     :properties {"code" {:type "string"
                                                          :description "Clojure/ClojureScript code to evaluate"}}
                                     :required ["code"]}}])

(defn- handle-request-fn [config {:keys [id method params] :as request}]
  (logging/debug! config "BOOM! handle-request " (pr-str request))
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
      (logging/debug! config "Formatted tools response:" (pr-str tools))
      response)

    (= method "tools/call")
    (let [{:keys [arguments]
           tool :name} params]
      (p/let [result (if (= tool "calva-eval")
                       (evaluate-code+ (:code arguments) js/undefined)
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

(defn- split-buffer-on-newline [buffer]
  (let [lines (str/split buffer #"\n")]
    (cond
      (empty? lines)
      [[] ""]

      ;; Buffer ends with newline - all segments are complete
      (str/ends-with? buffer "\n")
      [(filter (comp not str/blank?) lines) ""]

      :else
      ;; Last line is incomplete
      [(filter (comp not str/blank?) (butlast lines)) (last lines)])))

(defn- parse-request-json [json-str]
  (try
    (let [request-js (js/JSON.parse json-str)]
      (js->clj request-js :keywordize-keys true))
    (catch js/Error e
      {:error :parse-error :message (.-message e) :json json-str})))

(defn- format-response-json [response]
  (str (js/JSON.stringify (clj->js response)) "\n"))

(defn- create-error-response [id code message]
  {:jsonrpc "2.0" :id id :error {:code code :message message}})

(defn- process-segment [config segment handler]
  (let [request-json (str/trim segment)]
    (if (str/blank? request-json)
      (do
        (logging/debug! config "[Server] Blank line segment received, ignoring.")
        nil)
      (let [parsed (parse-request-json request-json)]
        (if (:error parsed)
          (do
            (logging/error! config "[Server] Error parsing request JSON segment:" (:message parsed) {:json (:json parsed)})
            (create-error-response nil -32700 "Parse error"))
          (do
            (logging/debug! config "[Server] Processing request for method:" (:method parsed))
            (handler parsed)))))))

(defn- process-segments [config segments handler]
  (keep #(process-segment config % handler) segments))

(defn- handle-socket-data! [config buffer-atom data-chunk handler]
  (let [_ (logging/debug! config "[Server] Socket received chunk:" data-chunk)
        _ (vswap! buffer-atom str data-chunk)
        [segments remainder] (split-buffer-on-newline @buffer-atom)
        _ (logging/debug! config "[Server] Split segments:" (pr-str segments) "Remainder:" (pr-str remainder))
        _ (vreset! buffer-atom remainder)
        responses (process-segments config segments handler)
        _ (logging/debug! config "[Server] Generated responses:" (pr-str responses))]
    responses))

(defn- setup-socket-handlers! [config ^js socket handler]
  (.setEncoding socket "utf8")
  (let [buffer (volatile! "")]
    (.on socket "data"
         (fn [data-chunk]
           (let [responses (handle-socket-data! config buffer data-chunk handler)]
             (doseq [response responses]
               (when response
                 (if (p/promise? response)
                   (-> response
                       (p/then (fn [resolved-response]
                                 (logging/debug! config "[Server] Sending resolved response for:" (pr-str resolved-response))
                                 (.write socket (format-response-json resolved-response))))
                       (p/catch (fn [err]
                                  (logging/error! config "[Server] Error resolving response:" err)
                                  (let [error-response (create-error-response nil -32603 (str "Internal error: " err))]
                                    (.write socket (format-response-json error-response))))))
                   (do
                     (logging/debug! config "[Server] Sending response for:" (pr-str response))
                     (.write socket (format-response-json response))))))))
    (.on socket "error"
         (fn [err]
           (logging/error! config "[Server] Socket error:" err))))))

(defn- create-request-handler [config]
  (fn [request]
    (handle-request-fn config request)))

(defn- start-socket-server!+ [config]
  (let [handle-request (create-request-handler config)]
    (p/create
     (fn [resolve-fn reject]
       (try
         (let [server (.createServer
                       net
                       (fn [^js socket]
                         (setup-socket-handlers! config socket handle-request)))]
           (.on server "error"
                (fn [err]
                  (logging/error! config "[Server] Server creation error:" err)
                  (reject err)))
           (.listen server 0
                    (fn []
                      (let [address (.address server)
                            port (.-port address)]
                        (logging/info! config "[Server] Socket server listening on port" port)
                        (resolve-fn {:server/instance server :server/port port})))))
         (catch js/Error e
           (logging/error! config "[Server] Error creating server:" (.-message e))
           (reject e)))))))

(defn start-server!+
  "Returns a promise that resolves to a map with server info when the MCP server starts successfully.
   Takes a config map with `:ex/dispatch!` and `:app/log-dir-uri`.
   Creates a socket server and writes the port to a file."
  [config]
  (p/let [server-info (start-socket-server!+ config)
          port (:server/port server-info)
          ^js port-file-uri (get-port-file-uri)]
    (if port-file-uri
      (p/do!
       (ensure-port-file-dir-exists!+)
       (.writeFile vscode/workspace.fs port-file-uri (js/Buffer.from (str port)))
       (logging/info! config "Wrote port file:" (.-fsPath port-file-uri))
       (assoc server-info :server/log-uri (logging/get-log-path config)))
      (do
        (logging/error! config "Could not determine workspace root to write port file.")
        (p/rejected (js/Error. "Could not determine workspace root"))))))

(defn- close-server!+ [config instance]
  (logging/info! config "Stopping socket server...")
  (p/create
    (fn [resolve-fn reject]
      (.close instance
        (fn [err]
          (if err
            (do
              (logging/error! config "Error stopping socket server:" err)
              (reject err))
            (do
              (logging/info! config "Socket server stopped.")
              (resolve-fn true))))))))

(defn stop-server!+
  "Returns a promise that resolves to a boolean indicating success.
   Takes a config map with `:ex/dispatch!`, `:app/log-dir-uri`, and `:server/instance`.
   Stops the MCP server and removes the port file."
  [{:keys [server/instance] :as config}]
  (if-not instance
    (do
      (logging/info! config "No server instance provided to stop.")
      (p/resolved false))
    (-> (close-server!+ config instance)
        (p/then (fn [_]
                  (let [port-file-uri (get-port-file-uri)]
                    (delete-port-file!+ config port-file-uri))))
        (p/then (fn [_] true))
        (p/catch (fn [err]
                   (logging/error! config "Error during server shutdown:" err)
                   false)))))

(comment
  ;; Todo fix working RCF
  ;; (start-server!+)
  ;; (stop-server!+)
  :rcf)

