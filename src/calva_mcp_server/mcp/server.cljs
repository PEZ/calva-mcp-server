(ns calva-mcp-server.mcp.server
  (:require [cljs.nodejs :as nodejs]
            [calva-mcp-server.mcp.logging :as log]
            [promesa.core :as p]))

(defonce ^js http (nodejs/require "http"))

(defn handler [^js req ^js res]
  (println "BOOM! handler")
  (let [url-obj (js/URL. (.-url req) "http://localhost")
        path (.-pathname url-obj)]
    (log/debug "MCP server request" {:path path})
    (cond
      (= path "/hello")
      (do
        (log/info "MCP server hello request")
        (doto res
          (.writeHead 200 #js {"Content-Type" "application/json"})
          (.end (js/JSON.stringify (clj->js {:message "Hello, World!"})))))

      (= path "/status")
      (do
        (log/info "MCP server status request")
        (doto res
          (.writeHead 200 #js {"Content-Type" "application/json"})
          (.end (js/JSON.stringify (clj->js {:connected true
                                             :session "cljs"
                                             :namespace "user"
                                             :version "0.1.0"})))))

      (= path "/capabilities")
      (do
        (log/info "MCP server capabilities request")
        (doto res
          (.writeHead 200 #js {"Content-Type" "application/json"})
          (.end (js/JSON.stringify (clj->js {:tools [{:name "hello-world"
                                                      :description "Returns a friendly greeting."
                                                      :parameters []
                                                      :endpoint "/hello"}]})))))

      :else
      (do
        (log/info "MCP server unknown request" {:path path})
        (doto res
          (.writeHead 404 #js {"Content-Type" "application/json"})
          (.end (js/JSON.stringify #js {:error "Not found"})))))))

(defonce server-instance (atom nil))

(defn start-server
  ([] (start-server 9000))
  ([port]
   (try
     (log/info "Starting MCP server" {:port port})
     (let [new-server (.createServer http (fn [^js req ^js res]
                                            (try
                                              (handler req res)
                                              (catch :default err
                                                (log/error "Handler error:" err)
                                                (when-not (unchecked-get res "headersSent")
                                                  (doto res
                                                    (.writeHead 500 #js {"Content-Type" "application/json"})
                                                    (.end (js/JSON.stringify #js {:error "Internal server error"}))))))))
           promise (p/create (fn [resolve reject]
                               (.on new-server "error" (fn [err]
                                                         (log/error "Server error:" err)
                                                         (reject err)))
                               (.listen new-server port (fn []
                                                          (log/info "MCP server listening on port" port)
                                                          (reset! server-instance new-server)
                                                          (resolve new-server)))))]
       promise)
     (catch :default err
       (log/error "Failed to start MCP server:" err)
       (p/rejected err)))))

(defn stop-server []
  (log/info "Stopping MCP server" @server-instance)
  (if-let [srv @server-instance]
    (p/create (fn [resolve reject]
                (try
                  (.close srv (fn [err]
                                (if err
                                  (do
                                    (log/error "Error stopping MCP server:" err)
                                    (reject err))
                                  (do
                                    (reset! server-instance nil)
                                    (log/info "MCP server stopped")
                                    (resolve nil)))))
                  (catch :default err
                    (log/error "Exception stopping MCP server:" err)
                    (reject err)))))
    (do
      (log/info "No MCP server instance to stop")
      (p/resolved nil))))

;; To start the server, call (start-server) or (start-server port)

(comment
  (start-server)
  (stop-server)
  :rcf)

