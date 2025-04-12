(ns e2e.db
  (:require [cljs.test]))

(def default-db {:running nil
                 :pass 0
                 :fail 0
                 :error 0})

(def !state (atom default-db))

