(ns hql.json
  (:require
    [jsonista.core :as json]))

(def ->str json/write-value-as-string)

(def <-str #(json/read-value % json/keyword-keys-object-mapper))

