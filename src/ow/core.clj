(ns ow.core
  (:use [ow.export :as e]
        [ow.import :as i]))

(def *ow-config* {:from-format :xml ;format of input owl file  (supported :xml, :turtle, :n3)
                  :to-format :xml ;format of output owl file  (supported :xml, :turtle, :n3)
                  :ontology-name "example"
                  :from-owl-location "/" ;location of owl files for import (transformation to mp model)
                  :to-owl-location "ow-export/" ;location where owl generated file will be set
                  :mp-domain-ns-generated "domain" ;ns of generated magic potion model 
                  :mp-domain-ns-source 'ow.my-domain ;'ow.test.examples.my-domain ;source ns of magic potion model (to be transformed to owl)
                  :ont-root-domain-ns "http://example.org/ontologies/"
                  :to-mp-location "ow-gen/"
                  :owl-file-for-import "ow-export/example.owl"
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
      (i/iimport new-config))))

