(ns leiningen.ow-export
  (:use [ow.core]
        [ow.util]
        [leininget.util]))

(defn ow-export
  [project]
  (let [config (get-config project)]
    (if config 
            (ow-export-goal config)
            (ow-export-goal))))