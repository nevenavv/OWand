(ns ow.test.examples.start
  (:use [ow.core]))

(defn export-example [] 
  (ow-export-goal {:mp-domain-package-source 'ow.test.examples.my-domain :ontology-name "nescafe"}))

