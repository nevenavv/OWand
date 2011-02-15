(ns leiningen.ow-import
  (:use [ow.core]
        [ow.util]
        [leiningen.util]))

(defn ow-import
  [project]
  (let [config (get-config project)]
    (if config 
            (ow-import-goal config)
            (ow-import-goal))))