(ns calva-mcp-server.mcp.server
  (:require
   ["net" :as net]
   ["vscode" :as vscode]
   [calva-mcp-server.mcp.logging :as log]
   [clojure.string :as str]
   [promesa.core :as p]))

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

(defn start-socket-server []
  (let [server (.createServer net (fn [^js socket]
                                    (.setEncoding socket "utf8")
                                    (let [buffer (atom "")]
                                      (.on socket "data" (fn [chunk]
                                                           (swap! buffer str chunk)
                                                           (when (str/ends-with? @buffer "\n")
                                                             (p/let [request (js/JSON.parse @buffer)
                                                                     response (handle-request request)]
                                                               (.write socket (str (js/JSON.stringify (clj->js response)) "\n"))
                                                               (reset! buffer "")))))
                                      (.on socket "error" (fn [err]
                                                            (log/error "Socket error:" err))))))]
    (.listen server 0 (fn []
                        (log/info "Socket server listening on port" (.-port (.address server)))))
    server))

(def socket-server (atom nil))

(defn start-server []
  (p/let [srv (start-socket-server)]
    (reset! socket-server srv)
    (.-port (.address srv))))


(defn stop-server []
  (if-let [srv @socket-server]
    (p/create (fn [resolve reject]
                (log/info "Stopping socket server...")
                (.close srv (fn [err]
                              (if err
                                (do
                                  (log/error "Error stopping socket server:" err)
                                  (reject err))
                                (do
                                  (log/info "Socket server stopped.")
                                  (reset! socket-server nil)
                                  (resolve true)))))))
    (do
      (log/info "Socket server not running.")
      (p/resolved false))))

(comment
  (start-server)
  (stop-server)
  :rcf)

