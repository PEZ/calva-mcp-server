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

(defn split-buffer-on-newline [buffer]
  (let [lines (str/split buffer #"\n")]
    (if (empty? lines)
      [[] ""]
      (let [complete (butlast lines)
            remainder (last lines)]
        [complete remainder]))))

(defn parse-request-json [json-str]
  (try
    (let [request-js (js/JSON.parse json-str)]
      (js->clj request-js :keywordize-keys true))
    (catch js/Error e
      {:error :parse-error :message (.-message e) :json json-str})))

(defn format-response-json [response]
  (str (js/JSON.stringify (clj->js response)) "\n"))

(defn create-error-response [id code message]
  {:jsonrpc "2.0" :id id :error {:code code :message message}})

(defn process-segment [log-uri segment handler]
  (let [request-json (str/trim segment)]
    (if (str/blank? request-json)
      (do
        (logging/debug! log-uri "[Server] Blank line segment received, ignoring.")
        nil)
      (let [parsed (parse-request-json request-json)]
        (if (:error parsed)
          (do
            (logging/error! log-uri "[Server] Error parsing request JSON segment:" (:message parsed) {:json (:json parsed)})
            (create-error-response nil -32700 "Parse error"))
          (do
            (logging/debug! log-uri "[Server] Processing request for method:" (:method parsed))
            (handler parsed)))))))

(defn process-segments [log-uri segments handler]
  (keep #(process-segment log-uri % handler) segments))

(defn handle-socket-data [log-uri buffer-atom chunk handler]
  (let [_ (logging/debug! log-uri "[Server] Socket received chunk:" chunk)
        _ (vswap! buffer-atom str chunk)
        [segments remainder] (split-buffer-on-newline @buffer-atom)
        _ (vreset! buffer-atom remainder)
        responses (process-segments log-uri segments handler)]
    responses))

(defn create-socket-data-handler [log-uri buffer-atom handler]
  (fn [chunk]
    (let [responses (handle-socket-data log-uri buffer-atom chunk handler)]
      responses)))

(defn setup-socket-handlers [log-uri ^js socket handler]
  (.setEncoding socket "utf8")
  (let [buffer (volatile! "")]
    (.on socket "data"
         (fn [chunk]
           (let [responses (handle-socket-data log-uri buffer chunk handler)]
             (doseq [response responses]
               (when response
                 (logging/debug! log-uri "[Server] Sending response for:" (pr-str response))
                 (.write socket (format-response-json response)))))))
    (.on socket "error"
         (fn [err]
           (logging/error! log-uri "[Server] Socket error:" err)))))

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
                         (setup-socket-handlers log-uri socket handle-request)))]
           (.on server "error"
                (fn [err]
                  (logging/error! log-uri "[Server] Server creation error:" err)
                  (reject err)))
           (.listen server 0
                    (fn []
                      (let [address (.address server)
                            port (.-port address)]
                        (logging/info! log-uri "[Server] Socket server listening on port" port)
                        (resolve-fn {:server/instance server :server/port port})))))
         (catch js/Error e
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

