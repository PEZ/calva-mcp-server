(ns calva-mcp-server.mcp.requests
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.integrations.calva.api :as calva]
   [clojure.string :as string]
   [promesa.core :as p]))

(defn- get-extension-version []
  (some-> (vscode/extensions.getExtension "betterthantomorrow.calva-mcp-server")
          .-packageJSON
          .-version))

(defn handle-request-fn [{:ex/keys [dispatch!] :as options}
                         {:keys [id method params] :as request}]
  (dispatch! [[:app/ax.log :debug "[Server] handle-request " (pr-str request)]])
  (cond
    (= method "initialize")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:serverInfo {:name "calva-mcp-server"
                                          :version (get-extension-version)}
                             :protocolVersion "2024-11-05"
                             :capabilities {:tools {:listChanged true}
                                            :resources {:listChanged true}}
                             :instructions "Use the `evaluate-clojure-code` tool to evaluate Clojure/ClojureScript code. There are also tools for getting symbol info and for getting clojuredocs.org info."
                             :description "Gives access to the Clojure REPL connection (via Calva) and Clojure namespace info (via Calva). Effectively turning the AI Agent into a Clojure Interactive Programmer."}}]
      response)

    (= method "tools/list")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:tools (cond-> [{:name "evaluate-clojure-code"
                                              :description "Evaluate Clojure code, enabling AI Interactive Programming. Also works with ClojureScript, Babashka, nbb, Joyride, Basilisp, and any nREPL enabled Clojure-ish enough language."
                                              :inputSchema {:type "object"
                                                            :properties {"code" {:type "string"
                                                                                 :description "Clojure/ClojureScript code to evaluate"}
                                                                         "namespace" {:type "string"
                                                                                      :description "Fully qualified namespace in which to evaluate the code. E.g. if calling functions in a file you are reading, it is probably the namespace of that file that should be provided."}
                                                                         "" {}}
                                                            :required ["code"]}}]

                                      (calva/exists-get-symbol-info?)
                                      (conj {:name "get-symbol-info"
                                             :description calva/description-symbol-info
                                             :inputSchema {:type "object"
                                                           :properties {"clojure-symbol" {:type "string"
                                                                                          :description "The symbol to look up clojuredocs.org info from."}
                                                                        "namespace" {:type "string"
                                                                                     :description "Fully qualified namespace in which to evaluate the code. E.g. if calling functions in a file you are reading, it is probably the namespace of that file that should be provided."}
                                                                        "session-key" {:type "string"
                                                                                       :description "One of `clj`, `cljs`, or `cljc`. For Clojure, ClojureScript, and Common, respectively. Often the same as the extension of the file you are working with."}}
                                                           :required ["clojure-symbol"  "session-key" "namespace"]}})

                                      (calva/exists-get-clojuredocs?)
                                      (conj {:name "get-clojuredocs"
                                             :description calva/description-clojure-docs
                                             :inputSchema {:type "object"
                                                           :properties {"clojure-symbol" {:type "string"
                                                                                          :description "The symbol to look up clojuredocs.org info from."}
                                                                        "namespace" {:type "string"
                                                                                     :description "Fully qualified namespace in which to evaluate the code. Often the namespace of that file you are working with."}}
                                                           :required ["clojure-symbol"]}}))}}]
      response)

    (= method "resources/templates/list")
    (let [response {:jsonrpc "2.0"
                    :id id
                    :result {:resourceTemplates (cond-> []
                                                  (calva/exists-get-symbol-info?)
                                                  (conj {:uriTemplate "/symbol-info/{symbol}@{session-key}@{namespace}"
                                                         :name "symbol-info"
                                                         :description calva/description-symbol-info
                                                         :mimeType "application/json"})

                                                  (calva/exists-get-clojuredocs?)
                                                  (conj {:uriTemplate "/clojuredocs/{symbol}"
                                                         :name "clojuredocs"
                                                         :description calva/description-clojure-docs
                                                         :mimeType "application/json"}))}}]
      response)

    (= method "resources/read")
    (let [{:keys [uri]} params]
      (cond
        (string/starts-with? uri "/symbol-info/")
        (p/let [[_ clojure-symbol session-key ns] (re-find #"^/symbol-info/([^@]+)@([^@]+)@(.+)$" uri)
                info (calva/get-symbol-info+ (merge options
                                                    {:calva/clojure-symbol clojure-symbol
                                                     :calva/session-key session-key
                                                     :calva/ns ns}))]
          {:jsonrpc "2.0"
           :id id
           :result {:contents [{:uri uri
                                :text (js/JSON.stringify info)}]}})

        (string/starts-with? uri "/clojuredocs/")
        (p/let [[_ clojure-symbol] (re-find #"^/clojuredocs/(.+)$" uri)
                info (calva/get-clojuredocs+ (merge options
                                                    {:calva/clojure-symbol clojure-symbol}))]
          {:jsonrpc "2.0"
           :id id
           :result {:contents [{:uri uri
                                :text (js/JSON.stringify info)}]}})

        :else
        {:jsonrpc "2.0"
         :id id
         :error {:code -32601
                 :message "Unknown resource URI"}}))

    (= method "tools/call")
    (let [{:keys [arguments]
           tool :name} params]
      (cond
        (= tool "evaluate-clojure-code")
        (p/let [{:keys [code]
                 ns :namespace} arguments
                result (calva/evaluate-code+ (merge options
                                                    {:calva/code code
                                                     :calva/session js/undefined}
                                                    (when ns
                                                      {:calva/ns ns})))]
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                               :text (js/JSON.stringify result)}]}})

        (= tool "get-symbol-info")
        (p/let [{:keys [clojure-symbol session-key]
                 ns :namespace} arguments
                clojure-docs (calva/get-clojuredocs+ (merge options
                                                            {:calva/clojure-symbol clojure-symbol
                                                             :calva/session-key session-key
                                                             :calva/ns ns}))]
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                               :text (js/JSON.stringify clojure-docs)}]}})

        (= tool "get-clojuredocs")
        (p/let [{:keys [clojure-symbol]} arguments
                clojure-docs (calva/get-clojuredocs+ (merge options
                                                            {:calva/clojure-symbol clojure-symbol}))]
          {:jsonrpc "2.0"
           :id id
           :result {:content [{:type "text"
                               :text (js/JSON.stringify clojure-docs)}]}})

        :else
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
