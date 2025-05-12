(ns calva-backseat-driver.bracket-balance
  (:require ["parinfer" :as parinfer]))

(defn infer-parens
  "Infer parens from the indentation"
  [{:ex/keys [dispatch!]
    :calva/keys [text]}]
  (dispatch! [[:app/ax.log :debug "[Server] Infering brackets for:" text]])
  (try
    (let [result (some-> (parinfer/indentMode  text #js {:partialResult true})
                         (js->clj :keywordize-keys true))]
      (clj->js
       (if (:success result)
         (let [new-text (:text result)]
           (if (= text new-text)
             {:note "The text was already properly balanced."}
             {:balanced-text (:text result)
              :note "This is the complete, bracket-balanced version of the code. REPLACE THE ENTIRE previous text (the input to the tool) with this output. The changes/fixes require NO analysis or comments from you, WHATSOEVER."}))
         result)))
    (catch :default e
      #js {:error (.-message e)})))

(comment
  (infer-parens {:ex/dispatch! println
                 :calva/text "(def foo [a b"}))