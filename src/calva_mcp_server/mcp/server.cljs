(ns calva-mcp-server.mcp.server
  (:require
   ["fs" :as fs]
   ["net" :as net]
   ["vscode" :as vscode]
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


(defn- delete-port-file!+ [{:ex/keys [dispatch!]} ^js port-file-uri]
  (p/create
   (fn [resolve-fn _reject]
     (if-not port-file-uri
       (resolve-fn true)
       (-> (vscode/workspace.fs.delete port-file-uri #js {:recursive false, :useTrash false})
           (p/then (fn [_]
                     (dispatch! [[:app/ax.log :info "[Server] Deleted port file:" (.-fsPath port-file-uri)]])
                     (resolve-fn true)))
           (p/catch (fn [err]
                      (dispatch! [[:app/ax.log :error "[Server] Could not delete port file with VS Code API:"
                                   err (.-message err)]])
                      ;; Probably VS Code API unavailable during shutdown - try Node fs fallback
                      (try
                        (let [fs-path (.-fsPath port-file-uri)]
                          (if (.existsSync fs fs-path)
                            (do
                              (.unlinkSync fs fs-path)
                              (dispatch! [[:app/ax.log :info "[Server] Deleted port file with fs fallback:" fs-path]])
                              (resolve-fn true))
                            (do
                              (dispatch! [[:app/ax.log :debug "[Server] Port file already gone (fs fallback check)"]])
                              (resolve-fn true))))
                        (catch js/Error fs-err
                          (dispatch! [[:app/ax.log :warn "[Server] Could not delete port file with fallback either:" fs-err (.-message fs-err)]])
                          (resolve-fn true))))))))))

(def ^:private ^js calvaExt (vscode/extensions.getExtension "betterthantomorrow.calva"))

(def ^:private ^js calvaApi (-> calvaExt
                                .-exports
                                .-v1
                                (js->clj :keywordize-keys true)))

(defn- evaluate-code+
  "Returns a promise that resolves to the result of evaluating Clojure/ClojureScript code.
   Takes a string of code to evaluate and an optional REPL session."
  [{:ex/keys [dispatch!]} code session]
  (p/let [output #js {:stdout (fn [msg]
                                (dispatch! [[:app/ax.log :debug "[Server] Stdout from evaluation:" msg]])
                                (dispatch! [[:mcp/ax.send-notification-params {:params {:type "stdout"
                                                                                        :message msg}}]]))
                      :stderr (fn [msg]
                                (dispatch! [[:app/ax.log :debug "[Server] Stderr from evaluation:" msg]])
                                (dispatch! [[:mcp/ax.send-notification-params {:params {:type "stderr"
                                                                                        :message msg}}]]))}
          result (-> (p/let [^js evaluation+ ((get-in calvaApi [:repl :evaluateCode])
                                              session code "user" output)]
                       (dispatch! [[:app/ax.log :debug "[Server] Evaluating code:" code]])
                       {:result (.-result evaluation+)
                        :ns (.-ns evaluation+)
                        :stdout (.-output evaluation+)
                        :stderr (.-errorOutput evaluation+)})
                     (p/catch (fn [err] ; For unknown reasons we end up here if en evaluation throws
                                        ; in the REPL. For now we send the error as the result like this...
                                (dispatch! [[:app/ax.log :debug "[Server] Evaluation failed:"
                                             err]])
                                {:result "nil"
                                 :stderr (pr-str err)})))]
    result))

(comment
  (p/let [p ((get-in calvaApi [:repl :evaluateCode]) js/undefined
                                                     "(throw (Exception. :foo))" "user"
                                                     #js {:stdout (fn [msg]
                                                                    (println "BOOM! stdout" msg))
                                                          :stderr (fn [msg]
                                                                    (println "BOOM! stderr" msg))})]
    (def p p))

  :rcf)

(def ^:private tools [{:name "evaluate-clojure-code"
                       :description "Evaluate Clojure/ClojureScript code, enabling AI Interactive Programming."
                       :inputSchema {:type "object"
                                     :properties {"code" {:type "string"
                                                          :description "Clojure/ClojureScript code to evaluate"}}
                                     :required ["code"]}
                       :audience ["user"]
                       :priority 1}])

(defn- handle-request-fn [{:ex/keys [dispatch!] :as options}
                          {:keys [id method params] :as request}]
  (dispatch! [[:app/ax.log :debug "[Server] handle-request " (pr-str request)]])
  (cond
    (= method "initialize")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:serverInfo {:name "calva-mcp-server"
                                          :version "0.0.1"}
                             :protocolVersion "2024-11-05"
                             :capabilities {:tools {:listChanged true}}
                             :instructions "Use the `evaluate-clojure-code` tool to evaluate Clojure/ClojureScript code."
                             :description "Gives access to the Clojure REPL connection (via Calva). Effectively turning the AI Agent into a Clojure Interactive Programmer."}}]
      response)

    (= method "tools/list")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:tools tools}}]
      response)

    (= method "tools/call")
    (let [{:keys [arguments]
           tool :name} params]
      (if (= tool "evaluate-clojure-code")
        (p/let [result (evaluate-code+ options (:code arguments) js/undefined)]
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                               :text (pr-str result)}]}})
        {:jsonrpc "2.0"
         :id id
         :error {:code -32601
                 :message "Unknown tool"}}))

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

(defn- process-segment [{:ex/keys [dispatch!]} segment handler]
  (let [request-json (str/trim segment)]
    (if (str/blank? request-json)
      (do
        (dispatch! [[:app/ax.log :debug "[Server] Blank line segment received, ignoring."]])
        nil)
      (let [parsed (parse-request-json request-json)]
        (if (:error parsed)
          (do
            (dispatch! [[:app/ax.log :error "[Server] Error parsing request JSON segment:" (:message parsed) {:json (:json parsed)}]])
            (create-error-response nil -32700 "Parse error"))
          (do
            (dispatch! [[:app/ax.log :debug "[Server] Processing request for method:" (:method parsed)]])
            (handler parsed)))))))

(defn- process-segments [options segments handler]
  (keep #(process-segment options % handler) segments))

(defn- handle-socket-data! [{:ex/keys [dispatch!] :as options}
                            buffer-atom data-chunk handler]
  (let [_ (dispatch! [[:app/ax.log :debug "[Server] Socket received chunk:" data-chunk]])
        _ (vswap! buffer-atom str data-chunk)
        [segments remainder] (split-buffer-on-newline @buffer-atom)
        _ (dispatch! [[:app/ax.log :debug "[Server] Split segments:" (pr-str segments) "Remainder:" (pr-str remainder)]])
        _ (vreset! buffer-atom remainder)
        responses (process-segments options segments handler)
        _ (dispatch! [[:app/ax.log :debug "[Server] Generated responses:" (pr-str responses)]])]
    responses))

(def ^:private active-sockets (atom #{}))

(defn- setup-socket-handlers! [{:ex/keys [dispatch!] :as options} ^js socket handler]
  (.setEncoding socket "utf8")
  (swap! active-sockets conj socket)
  (.on socket "close" (fn [] (swap! active-sockets disj socket)))

  (let [buffer (volatile! "")]
    (.on socket "data"
         (fn [data-chunk]
           (let [responses (handle-socket-data! options buffer data-chunk handler)]
             (doseq [response responses]
               (when response
                 (if (p/promise? response)
                   (-> response
                       (p/then (fn [resolved-response]
                                 (dispatch! [[:app/ax.log :debug
                                              "[Server] Sending resolved response:"
                                              (pr-str resolved-response)]])
                                 (.write socket (format-response-json resolved-response))))
                       (p/catch (fn [err]
                                  (dispatch! [[:app/ax.log :error
                                               "[Server] Error resolving response:" err]])
                                  (let [error-response (create-error-response nil -32603 (str "Internal error: " err))]
                                    (.write socket (format-response-json error-response))))))
                   ;; Handle non-promise responses
                   (do
                     (dispatch! [[:app/ax.log :debug
                                  "[Server] Sending response:"
                                  (pr-str response)]])
                     (.write socket (format-response-json response))))))))
         (.on socket "error"
              (fn [err]
                (dispatch! [[:app/ax.log :error "[Server] Socket error:" err]]))))))

(defn- create-request-handler [options]
  (fn [request]
    (handle-request-fn options request)))

(defn- start-socket-server!+ [{:ex/keys [dispatch!] :as options}]
  (let [handle-request (create-request-handler options)]
    (p/create
     (fn [resolve-fn reject]
       (try
         (let [server (.createServer
                       net
                       (fn [^js socket]
                         (setup-socket-handlers! options socket handle-request)))]
           (.on server "error"
                (fn [err]
                  (dispatch! [[:app/ax.log :error "[Server] Server creation error:" err]])
                  (reject err)))
           (.listen server 0
                    (fn []
                      (let [address (.address server)
                            port (.-port address)]
                        (dispatch! [[:app/ax.log :info "[Server] Socket server listening on port" port]])
                        (resolve-fn {:server/instance server :server/port port})))))
         (catch js/Error e
           (dispatch! [[:app/ax.log :error "[Server] Error creating server:" (.-message e)]])
           (reject e)))))))

(defn send-notification-params [{:ex/keys [dispatch!]} params]
  (let [sockets @active-sockets]
    (when (seq sockets)
      (doseq [socket sockets]
        (try
          (.write socket (str (js/JSON.stringify (clj->js {:jsonrpc "2.0"
                                                           :method "notifications/progress"
                                                           :params params})) "\n"))
          (catch js/Error e
            (dispatch! [:app/ax.log :error "[Server] Error sending notification:" (.-message e)])))))))

(defn start-server!+
  "Returns a promise that resolves to a map with server info when the MCP server starts successfully.
   Creates a socket server and writes the port to a file."
  [{:ex/keys [dispatch!] :as options}]
  (p/let [server-info (start-socket-server!+ options)
          port (:server/port server-info)
          ^js port-file-uri (get-port-file-uri)]
    (if port-file-uri
      (p/do!
       (ensure-port-file-dir-exists!+)
       (.writeFile vscode/workspace.fs port-file-uri (js/Buffer.from (str port)))
       (dispatch! [[:app/ax.log :info "Wrote port file:" (.-fsPath port-file-uri)]])
       server-info)
      (do
        (dispatch! [[:app/ax.log :error "[Server] Could not determine workspace root to write port file."]])
        (p/rejected (js/Error. "Could not determine workspace root"))))))

(defn- close-server!+ [{:ex/keys [dispatch!]
                        :server/keys [instance]}]
  (dispatch! [[:app/ax.log :info "Stopping socket server..."]])
  (-> (p/create
       (fn [resolve-fn reject]
         (when (seq @active-sockets)
           (dispatch! [[:app/ax.log :info "Closing all active socket connections (" (count @active-sockets) ")..."]])
           (doseq [^js socket @active-sockets]
             (try
               (.end socket)
               (.destroy socket)
               (catch js/Error e
                 (dispatch! [[:app/ax.log :warn "[Server] Error closing socket:" (.-message e)]])))))
         (reset! active-sockets #{})
         (.close instance
                 (fn [err]
                   (if err
                     (do
                       (dispatch! [[:app/ax.log :error "[Server] Error stopping socket server:" err]])
                       (reject err))
                     (do
                       (dispatch! [[:app/ax.log :info "Socket server stopped."]])
                       (resolve-fn true)))))))
      (p/catch (fn [err2]
                 (dispatch! [[:app/ax.log :error "[Server] Error stopping socket server:" err2]])))))

(defn stop-server!+
  "Returns a promise that resolves to a boolean indicating success.
   Stops the MCP server and removes the port file."
  [{:keys [server/instance ex/dispatch!] :as options}]
  (if-not instance
    (do
      (dispatch! [[:app/ax.log :info "No server instance provided to stop."]])
      (p/resolved false))
    (-> (close-server!+ options)
        (p/then (fn [_]
                  (let [port-file-uri (get-port-file-uri)]
                    (delete-port-file!+ options port-file-uri))))
        (p/then (fn [_] true))
        (p/catch (fn [err]
                   (dispatch! [[:app/ax.log :error "[Server] Error during server shutdown:" err]])
                   false)))))

(comment
  ;; Todo fix working RCF
  ;; (start-server!+)
  ;; (stop-server!+)
  :rcf)

