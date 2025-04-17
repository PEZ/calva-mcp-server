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
                       :parameters [{:name "code" :type "string"}]}])

(defn create-request-handler [log-uri]
  (fn handle-request [{:keys [id method params] :as request}]
    (logging/debug! log-uri "BOOM! handle-request " (pr-str request))
    (cond
      (= method "initialize")
      (let [response {:jsonrpc "2.0"
                      :id id
                      :result {:serverInfo {:name "CalvaMCP"
                                            :version "0.1.0"}
                               :capabilities {:name "CalvaMCP"
                                              :version "0.1.0"
                                              :tools tools}}}]
        response)

      (= method "tools/list")
      (let [response {:jsonrpc "2.0"
                      :id id
                      :result tools}]
        response)

      (= method "invokeTool")
      (let [{:keys [tool params]} params]
        (p/let [response (if (= tool "calva-eval")
                           (p/let [result (evaluate-code (:code params) js/undefined)]
                             {:jsonrpc "2.0" :id id :result result})
                           {:jsonrpc "2.0" :id id :error {:code -32601 :message "Unknown tool"}})]
          response))

      id
      {:jsonrpc "2.0" :id id :error {:code -32601 :message "Method not found"}}

      :else
      nil)))

(defn start-socket-server [log-uri]
  (let [handle-request (create-request-handler log-uri)]
    (p/create
     (fn [resolve-fn reject]
       (try
         (let [server (.createServer
                       net
                       (fn [^js socket]
                         (.setEncoding socket "utf8")
                         (let [buffer (volatile! "")]
                           (.on socket "data"
                                (fn [a-chunk]
                                  (vswap! buffer str a-chunk)
                                  (when (str/ends-with? @buffer "\n")
                                    (try
                                      (p/let [request-js (js/JSON.parse @buffer)
                                              {:keys [method]
                                               :as request} (js->clj request-js
                                                                     :keywordize-keys true)
                                              response (handle-request request)]
                                        (if response
                                          (do
                                            (logging/debug! log-uri
                                                            "BOOM! sending '"
                                                            method
                                                            "' response "
                                                            (pr-str response))
                                            (.write socket (str (js/JSON.stringify (clj->js response)) "\n")))
                                          (logging/debug! log-uri
                                                          "BOOM! not sending response for '"
                                                          method "' "
                                                          (pr-str response)))
                                        (vreset! buffer ""))
                                      (catch :default parse-err
                                        (logging/error! log-uri
                                                        "Error parsing request JSON:"
                                                         parse-err {:buffer @buffer})
                                        (.write socket (str (js/JSON.stringify
                                                             #js {:jsonrpc "2.0"
                                                                  :error #js {:code -32700
                                                                              :message "Parse error"}})
                                                            "\n"))
                                        (vreset! buffer ""))))))
                           (.on socket "error" (fn [err]
                                                 (logging/error! "Socket error:" err))))))]
           (.on server "error" (fn [err]
                                 (logging/error! "Server creation error:" err)
                                 (reject err)))
           (.listen server 0 (fn []
                               (let [address (.address server)
                                     port (.-port address)]
                                 (logging/info! log-uri "Socket server listening on port" port)
                                 (resolve-fn {:server/instance server :server/port port})))))
         (catch :default e
           (logging/error! log-uri "Error creating server:" e)
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

