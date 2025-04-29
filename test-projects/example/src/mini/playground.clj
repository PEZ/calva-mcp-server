(ns mini.playground
  (:require
   [clojure.string :as str]))


(defn inlieu [ch]
  (fn [m] (apply str (repeat (count m) ch))))

(defn inlieu2 [c]
  #(str/replace % #"." c))

(def text
  "
(ns mini.playground
  (:require
   [clojure.string :as str]))")

(print (str/replace text #"(?m)^ +" (inlieu "-")))

(print (str/replace text #" +" (inlieu "-")))

(print (str/replace text #"(?m)^ +" #(str/replace % #"." "-")))

(print (str/replace text #"(?m)^ +" (inlieu2 "-")))
