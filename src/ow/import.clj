(ns ow.import
  (:use [plaza.rdf.core]
        [plaza.rdf.implementations.jena]
        [plaza.rdf.sparql]
        [plaza.rdf.predicates]
        [ow.util]))

(defn modeling
  [file doc-format]
  (let [m (build-model :jena)
        _m (with-model m (document-to-model  (java.io.FileInputStream. file) doc-format))
        t (model-to-triples m)]
    (println "Number of triples:" (count t))))
                       

(defn iimport
  [{doc-format :from-format gen-folder :to-mp-location mp-ns :mp-domain-ns-generated
    from-file :owl-file-for-import}]
  (let [[folder file-name] (ff-name gen-folder mp-ns)
        mp-ns-sym (symbol mp-ns)]
    (modeling from-file doc-format)
    (create-file folder (str file-name ".clj")
      (with-out-str (pr '(ns mp-ns-sym))))))