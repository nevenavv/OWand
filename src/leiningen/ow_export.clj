(ns leiningen.ow-export
  (:use [ow.core]
        [leiningen.util]))

(defn ow-export
  [project]
  (if-let [config (get-config project)]
		(ow-export-goal config)
		(ow-export-goal)))