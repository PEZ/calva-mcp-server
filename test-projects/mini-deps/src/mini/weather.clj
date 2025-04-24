(ns mini.weather
  (:require [clojure.string :as str]))

;; Created: April 24, 2025
;; Created by: GitHub Copilot (pair programming with Peter Strömberg)
;;
;; This weather utility demonstrates the power of Clojure's higher-order functions.
;; It was created during testing of the Calva MCP server to showcase interactive
;; programming practices using map, juxt, comp, and partial for data transformation.

;; Weather Data Processing Utility
;; A demonstration of combining map, juxt, comp, and partial

(def sample-weather-data
  [{:city "Stockholm" :temp 15 :conditions "Partly Cloudy" :humidity 60}
   {:city "London" :temp 12 :conditions "Rainy" :humidity 85}
   {:city "Barcelona" :temp 24 :conditions "Sunny" :humidity 40}
   {:city "New York" :temp 18 :conditions "Cloudy" :humidity 65}
   {:city "Tokyo" :temp 22 :conditions "Clear" :humidity 50}])

;; Using juxt to extract multiple properties at once
(def city-and-temp (juxt :city :temp))

;; Using partial to create a temperature conversion function
(def c-to-f (partial (fn [temp] (+ (* temp 1.8) 32))))

;; Using comp to create a temperature description function
(def temp-description
  (comp (fn [f] (str f "°F"))
        (partial (fn [temp] (+ (* temp 1.8) 32)))
        :temp))

;; Using comp with keywords to navigate nested data
(defn weather-report [data]
  (let [humid? #(> % 70)]  ;; Using a clearer function definition
    (->> data
         ;; Using map with anonymous functions
         (map (fn [city-data]
                (assoc city-data
                       :temp-f (c-to-f (:temp city-data))
                       :humid (humid? (:humidity city-data)))))
         ;; Using juxt to extract and group data
         (group-by (comp #(if % "Humid" "Dry") :humid))
         ;; Using comp and partial for data transformation
         (map (fn [[humidity cities]]
                [humidity (map (juxt :city temp-description) cities)])))))

;; Using comp to create a formatting function
(def format-weather-report
  (comp (partial str/join "\n\n")
        (partial map
                 (fn [[humidity cities]]
                   (str humidity " Cities:\n"
                        (str/join "\n"
                                  (map (fn [[city temp]]
                                         (str "  " city ": " temp))
                                       cities)))))))

(comment
  ;; Evaluate weather-report with our sample data
  (weather-report sample-weather-data)

  ;; Format and print the result for a nice display
  (println (format-weather-report (weather-report sample-weather-data)))

  ;; Try a combined approach with all four functions
  (->> sample-weather-data
       (map (fn [city]
              ((juxt identity
                    (comp (partial str "Weather in ")
                          (partial str/upper-case)
                          :city))
               city)))
       (map (fn [[data city-title]]
              (str city-title ": "
                   (:conditions data) ", "
                   (:temp data) "°C")))
       (str/join "\n")
       println)
)