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

(defn- get-port-file-uri []
  (when-let [^js root-uri (get-workspace-root-uri)]
    (vscode/Uri.joinPath root-uri ".calva" "mcp-server" "port")))

(defn- ensure-port-file-dir-exists []
  (when-let [^js root-uri (get-workspace-root-uri)]
    (let [dir-uri (vscode/Uri.joinPath root-uri ".calva" "mcp-server")]
      (.createDirectory vscode/workspace.fs dir-uri))))

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

(defn handle-request [request]
  (let [{:keys [id method params]} (js->clj request :keywordize-keys true)]
    (cond
      (= method "initialize")
      {:jsonrpc "2.0"
       :id id
       :result {:capabilities {:name "CalvaMCP"
                               :version "0.1.0"
                               :tools [{:name "calva-eval"
                                        :description "Evaluate Clojure/ClojureScript code"
                                        :parameters [{:name "code" :type "string"}]}]}}}

      (= method "listTools")
      {:jsonrpc "2.0"
       :id id
       :result [{:name "calva-eval"
                 :description "Evaluate Clojure/ClojureScript code"
                 :parameters [{:name "code" :type "string"}]}]}

      (= method "invokeTool")
      (let [{:keys [tool params]} params]
        (if (= tool "calva-eval")
          (p/let [result (evaluate-code (:code params) js/undefined)]
            {:jsonrpc "2.0" :id id :result result})
          {:jsonrpc "2.0" :id id :error {:code -32601 :message "Unknown tool"}}))

      :else
      {:jsonrpc "2.0" :id id :error {:code -32601 :message "Method not found"}})))

(defn start-socket-server [log-uri]
  (p/create
   (fn [resolve reject]
     (try
       (let [server (.createServer net
                                   (fn [^js socket]
                                     (.setEncoding socket "utf8")
                                     (let [buffer (atom "")]
                                       (.on socket "data"
                                            (fn [chunk]
                                              (swap! buffer str chunk)
                                              (when (str/ends-with? @buffer "\n")
                                                (try
                                                  (p/let [request (js/JSON.parse @buffer)
                                                          response (handle-request request)]
                                                    (.write socket (str (js/JSON.stringify (clj->js response)) "\n"))
                                                    (reset! buffer ""))
                                                  (catch :default parse-err
                                                    (logging/error!  "Error parsing request JSON:" parse-err {:buffer @buffer})
                                                    (.write socket (str (js/JSON.stringify #js {:jsonrpc "2.0", :error #js {:code -32700, :message "Parse error"}}) "\n"))
                                                    (reset! buffer ""))))))
                                       (.on socket "error" (fn [err]
                                                             (logging/error! "Socket error:" err))))))]
         (.on server "error" (fn [err]
                               (logging/error! "Server creation error:" err)
                               (reject err)))
         (.listen server 0 (fn []
                             (let [address (.address server)
                                   port (.-port address)]
                               (logging/info! log-uri "Socket server listening on port" port)
                               (resolve {:server/instance server :server/port port})))))
       (catch :default e
         (logging/error! log-uri "Error creating server:" e)
         (reject e))))))

(defn start-server [{:app/keys [log-uri]}]
  (p/let [server-info (start-socket-server log-uri)
          port (:server/port server-info)
          ^js port-file-uri (get-port-file-uri)]
    (if port-file-uri
      (p/do!
       (ensure-port-file-dir-exists)
       (.writeFile vscode/workspace.fs port-file-uri (js/Buffer.from (str port)))
       (logging/info! log-uri "Wrote port file:" (.-fsPath port-file-uri))
       server-info)
      (do
        (logging/error! log-uri "Could not determine workspace root to write port file.")
        (p/rejected (js/Error. "Could not determine workspace root"))))))

(defn stop-server [{:keys [app/log-uri server/instance]}]
  (if instance
    (p/let [^js port-file-uri (get-port-file-uri)
            _ (logging/info! log-uri "Stopping socket server...")]
      (-> (p/create (fn [resolve reject]
                      (.close instance (fn [err]
                                         (if err
                                           (do
                                             (logging/error! log-uri "Error stopping socket server:" err)
                                             (reject err))
                                           (do
                                             (logging/info! log-uri "Socket server stopped.")
                                             (resolve true)))))))
          (p/then (fn [_]
                    (if port-file-uri
                      (-> (.delete vscode/workspace.fs port-file-uri #js {:recursive false, :useTrash false})
                          (p/then (fn [_] (logging/info! log-uri "Deleted port file:" (.fsPath port-file-uri)) true))
                          (p/catch (fn [_del-err]
                                     (logging/warn! "Could not delete port file (maybe already gone?).")
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

