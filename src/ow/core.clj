(ns ow.core
  (:require [ow.export :as e]
            [ow.import :as i]))

(def *ow-config* {:ontology-name "example"
                  :ont-root-domain-ns "http://example.org/ontologies/"
                  :from-owl-format :xml ;format of input owl file  (supported :xml, :turtle, :n3)
                  :to-owl-format :xml ;format of output owl file (supported :xml, :turtle, :n3)
                  :to-owl-dir "ow-export/" ;location where owl generated file will be set
                  :to-mp-dir "ow-gen/"
                  :owl-file-for-import "ow-export/example.owl"
                  :mp-domain-ns-source "ow.my-domain" ;ow.test.examples.my-domain ;source ns of magic potion model (to be transformed to owl)
                  :mp-domain-gen-ns "domain" ;ns of generated magic potion model 
                  })

(def *ow-cc* *ow-config*)

(defn owc-update-default
  "Update OW configuration with provided key value pairs"
  [pairs]
  {:pre [(map? pairs)]}
  (alter-var-root #'*ow-config* merge pairs))

(defn owc-reset-to-default 
  "Reset OW configuration to default."
  []
  (owc-update-default *ow-cc*))
  
;; goals ------------------------------------------

(defn ow-export-goal
  "Exports Magic Potion model to OWL file."
  ([]
    (println "Exporting with default configuration...")
    (ow-export-goal *ow-config*))
  ([config-map]
    {:pre [(map? config-map)]}
    (let [new-config (merge *ow-config* config-map)]
      (println "Exporting...")
      (e/export new-config))))

(defn ow-import-goal
  "Generates Magic Potion model form OWL file."
  ([]
    (println "Importing with default configuration...")
    (ow-import-goal *ow-config*))
  ([config-map]
    {:pre [(map? config-map)]}
    (let [new-config (merge *ow-config* config-map)]
      (println "Importing...")
      (i/ow-import new-config))))

