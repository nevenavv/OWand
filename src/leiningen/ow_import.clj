(ns leiningen.ow-import
  (:use [ow.core]
        [leiningen.util]))

(defn ow-import
  [project]
  (if-let [config (get-config project)]
		(ow-import-goal config)
		(ow-import-goal)))