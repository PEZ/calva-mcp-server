(ns calva-backseat-driver.integrations.vscode.tools
  (:require
   ["vscode" :as vscode]
   [calva-backseat-driver.integrations.calva.api :as calva]
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
                 (p/let [code (.-code (.-input options))
                         ns (.-namespace (.-input options))
                         session-key (.-replSessionKey (.-input options))
                         result (calva/evaluate-code+ {:ex/dispatch! dispatch!
                                                       :calva/code code
                                                       :calva/ns ns
                                                       :calva/repl-session-key session-key})]
                   (vscode/LanguageModelToolResult.
                    #js [(vscode/LanguageModelTextPart.
                          (js/JSON.stringify (clj->js result)))])))})

(defn GetSymbolInfoTool [dispatch!]
  #js {:prepareInvocation (fn prepareInvocation [^js options _token]
                            (let [symbol (-> options .-input .-clojureSymbol)
                                  message (str "Get info for Clojure symbol: **" symbol "**")]
                              #js {:invocationMessage "Getting symbol info"
                                   :confirmationMessages #js {:title "Get Symbol Info"
                                                              :message message}}))

       :invoke (fn invoke [^js options _token]
                 (p/let [symbol (-> options .-input .-clojureSymbol)
                         ns (-> options .-input .-namespace)
                         session-key (-> options .-input .-sessionKey)
                         result (calva/get-symbol-info+ {:ex/dispatch! dispatch!
                                                         :calva/clojure-symbol symbol
                                                         :calva/ns ns
                                                         :calva/repl-session-key session-key})]
                   (vscode/LanguageModelToolResult.
                    #js [(vscode/LanguageModelTextPart.
                          (js/JSON.stringify (clj->js result)))])))})

(defn GetClojureDocsTool [dispatch!]
  #js {:prepareInvocation (fn prepareInvocation [^js options _token]
                            (let [symbol (-> options .-input .-clojureSymbol)
                                  message (str "Look up docs for Clojure symbol: **" symbol "**")]
                              #js {:invocationMessage "Looking up ClojureDocs"
                                   :confirmationMessages #js {:title "Get ClojureDocs Info"
                                                              :message message}}))

       :invoke (fn invoke [^js options _token]
                 (p/let [symbol (-> options .-input .-clojureSymbol)
                         result (calva/get-clojuredocs+ {:ex/dispatch! dispatch!
                                                         :calva/clojure-symbol symbol})]
                   (vscode/LanguageModelToolResult.
                    #js [(vscode/LanguageModelTextPart.
                          (js/JSON.stringify (clj->js result)))])))})

(defn GetOutputLogTool [dispatch!]
  #js {:prepareInvocation (fn prepareInvocation [^js options _token]
                            (let [since-line (-> options .-input .-sinceLine)
                                  message (str "Fetch REPL output from line " since-line)]
                              #js {:invocationMessage "Fetching REPL output"
                                   :confirmationMessages #js {:title "Get REPL Output Log"
                                                              :message message}}))

       :invoke (fn invoke [^js options _token]
                 (let [since-line (-> options .-input .-sinceLine)
                       result (calva/get-output {:ex/dispatch! dispatch!
                                                 :calva/since-line since-line})]
                   (vscode/LanguageModelToolResult.
                    #js [(vscode/LanguageModelTextPart.
                          (js/JSON.stringify (clj->js result)))])))})

(defn register-language-model-tools [dispatch!]
  (cond-> []
    :always
    (conj (vscode/lm.registerTool
           "evaluate_clojure_code"
           (#'EvaluateClojureCodeTool dispatch!)))

    (calva/exists-get-symbol-info?)
    (conj (vscode/lm.registerTool
           "get_symbol_info"
           (#'GetSymbolInfoTool dispatch!)))

    (calva/exists-get-clojuredocs?)
    (conj (vscode/lm.registerTool
           "get_clojuredocs_info"
           (#'GetClojureDocsTool dispatch!)))

    (calva/exists-on-output?)
    (conj (vscode/lm.registerTool
           "get_repl_output_log"
           (#'GetOutputLogTool dispatch!)))))
