(ns calva-mcp-server.integrations.vscode.tools
  (:require
   ["vscode" :as vscode]
   [calva-mcp-server.integrations.calva.api :as calva]
   [promesa.core :as p]))

(defn EvaluateClojureCodeTool [dispatch!]
  #js {:prepareInvocation (fn prepareInvocation [^js options _token]
                            (let [code (-> options .-input .-code)
                                  ns (-> options .-input .-namespace)
                                  session-key (-> options .-input .-replSessionKey)
                                  message (str "Evaluate?\n```clojure\n(in-ns " ns ")\n\n" code "\n```")]
                              #js {:invocationMessage "Evaluating code"
                                   :confirmationMessages #js {:title (str "Evaluate code in the **" session-key "** REPL")
                                                              :message message}}))

       :invoke (fn invoke [^js options _token]
                 (def options options)
                 (p/let [code (.-code (.-input options))
                         ns (.-namespace (.-input options))
                         session-key (.-replSessionKey (.-input options))
                         result (calva/evaluate-code+ {:ex/dispatch! dispatch!
                                                       :calva/code code
                                                       :calva/ns ns
                                                       :calva/repl-session-key session-key})]
                   (def result result)
                   (vscode/LanguageModelToolResult.
                    #js [(vscode/LanguageModelTextPart.
                          (js/JSON.stringify (clj->js result)))])))})

(comment
  (def session-key "clj")
  (def ns "user")
  (def code ":foo")
  :rcf)

#_(deftype GetSymbolInfoTool []
  vscode/LanguageModelTool
  (prepareInvocation [_this options _token]
    (let [symbol (.-clojureSymbol (.-input options))
          message (str "Get info for Clojure symbol: **" symbol "**")]
      (vscode/LanguageModelToolInvocationMessage. message)))

  (invoke [_this ^js options token]
    (p/let [symbol (.-clojureSymbol (.-input options))
            ns (.-namespace (.-input options))
            session-key (.-sessionKey (.-input options))
            result (calva/get-symbol-info+ {:calva/clojure-symbol symbol
                                            :calva/ns ns
                                            :calva/repl-session-key session-key})]
      (vscode/LanguageModelToolResult.
       (array
        (vscode/LanguageModelTextPart.
         (js/JSON.stringify (clj->js result))))))))

#_(deftype GetClojureDocssTool []
  vscode/LanguageModelTool
  (prepareInvocation [_this ^js options _token]
    (let [symbol (.-clojureSymbol (.-input options))
          message (str "Look up docs for Clojure symbol: **" symbol "**")]
      (vscode/LanguageModelToolInvocationMessage. message)))

  (invoke [_this options token]
    (p/let [symbol (.-clojureSymbol (.-input options))
            result (calva/get-clojuredocs+ {:calva/clojure-symbol symbol})]
      (vscode/LanguageModelToolResult.
       (array
        (vscode/LanguageModelTextPart.
         (js/JSON.stringify (clj->js result))))))))

#_(deftype GetOutputLogTool []
  vscode/LanguageModelTool
  (prepareInvocation [_this ^js options _token]
    (let [since-line (-> options .-input .-sinceLine)
          message (str "Fetch REPL output from line " since-line)]
      (vscode/LanguageModelToolInvocationMessage. message)))

  (invoke [_this ^js options token]
    (p/let [since-line (-> options .-input .-sinceLine)
            result (calva/get-output {:calva/since-line since-line})]
      (vscode/LanguageModelToolResult.
       (array
        (vscode/LanguageModelTextPart.
         (js/JSON.stringify (clj->js result))))))))

(defn register-language-model-tools [dispatch!]
  (cond-> []
    :always
    (conj (vscode/lm.registerTool
           "evaluate_clojure_code"
           (EvaluateClojureCodeTool. dispatch!)))

    #_#_(calva/exists-get-symbol-info?)
      (conj (vscode/lm.registerTool
             "get_symbol_info"
             (GetSymbolInfoTool.)))

    #_#_(calva/exists-get-clojuredocs?)
      (conj (vscode/lm.registerTool
             "get_clojuredocs_info"
             (GetClojureDocssTool.)))

    #_#_(calva/exists-on-output?)
      (conj (vscode/lm.registerTool
             "get_repl_output_log"
             (GetOutputLogTool.)))))
