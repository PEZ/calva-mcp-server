(ns calva-backseat-driver.integrations.parinfer
  (:require ["parinfer" :as parinfer]))

(defn infer-brackets [text]
  (parinfer/indentMode text #js {:partialResult true}))