(ns calva-mcp-server.mcp.server
  (:require [cljs.nodejs :as nodejs]))

(defonce ^js http (nodejs/require "http"))

(defn handler [^js req ^js res]
  (println "BOOM! handler")
  (let [url-obj (js/URL. (.-url req) "http://localhost")
        path (.-pathname url-obj)]
    (cond
      (= path "/hello")
      (do
        (.writeHead res 200 #js {"Content-Type" "application/json"})
        (.end res (js/JSON.stringify (clj->js {:message "Hello, World!"}))))
      (= path "/status")
      (do
        (.writeHead res 200 #js {"Content-Type" "application/json"})
        (.end res (js/JSON.stringify (clj->js {:connected true
                                               :session "cljs"
                                               :namespace "user"
                                               :version "0.1.0"}))))
      (= path "/capabilities")
      (do
        (.writeHead res 200 #js {"Content-Type" "application/json"})
        (.end res (js/JSON.stringify (clj->js {:tools [{:name "hello-world"
                                                        :description "Returns a friendly greeting."
                                                        :parameters []
                                                        :endpoint "/hello"}]}))))
      :else
      (do
        (.writeHead res 404 #js {"Content-Type" "application/json"})
        (.end res (js/JSON.stringify #js {:error "Not found"}))))))

(defonce ^js server
  (.createServer http (fn [req res]
                        (handler req res))))

(defn start-server
  ([] (start-server 3000))
  ([port]
   (.listen server port #(js/console.log "MCP server listening on port" port))))

(defn stop-server []
  (when server
    (.close server #(js/console.log "MCP server stopped"))))

;; To start the server, call (start-server) or (start-server port)

(comment
  (start-server)
  (stop-server)
  :rcf)

