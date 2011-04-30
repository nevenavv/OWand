(ns leiningen.util
  (:use [ow.util]))

(defn get-config [project]
  (let [config (:ow-config project)
        url (:url project)
        nm (:name project)]
    (-> config
      (assoc-new :ont-root-domain-ns (str (trim-ending-str url "/") "/ontologies/"))
      (assoc-new :ontology-name nm))))
