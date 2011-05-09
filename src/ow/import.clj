(ns ow.import
  (:use [plaza.rdf.core]
        [plaza.rdf.implementations.jena]
        [plaza.rdf.sparql]
        [plaza.rdf.predicates]
        [clojure.contrib.pprint]
        [clojure.contrib.with-ns]
        [ow.util]))

(init-jena-framework)
(register-rdf-ns :owl "http://www.w3.org/2002/07/owl#")
(register-rdf-ns :rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#")

(defn modeling
  [file doc-format]
  (let [m (build-model :jena)
        _m (with-model m (document-to-model  (java.io.FileInputStream. file) doc-format))
        t (model-to-triples m)]
    {:dt-properties (map #(assoc {:type :property} :name ((comp qname-local :?x) %))
                      (model-query m (defquery
                                       (query-set-type :select)
                                       (query-set-vars [:?x])
                                       (query-set-pattern
                                         (make-pattern [[:?x [:rdf :type] [:owl :DatatypeProperty]]])))))}))
     
;; pprint -----------------------------------

(def holder)

;;binding & with-ns is necessary because pprint-logical-block macro is
;;using private var form pprint namespace
(defn pprint-mp-ns [nsp]
  (binding [holder nsp]
    (with-ns 'clojure.contrib.pprint
      (pprint-logical-block :prefix "(" :suffix ")"
        (.write #^java.io.Writer *out* "ns ")
        (print (:name ow.import/holder))
        (pprint-indent :block 1)
        (pprint-newline :mandatory)
        (pprint-logical-block :prefix "(:use " :suffix ")"
          (loop [alis (seq (:use ow.import/holder))]
            (when alis
              (print (str "[" (first alis) "]"))
              (when (next alis)
               (pprint-newline :mandatory)
                (recur (next alis)))))))
        (pprint-newline :mandatory)))) 

(defn pprint-mp-concept [concept]
  (binding [holder concept]
    (with-ns 'clojure.contrib.pprint
      (pprint-logical-block :prefix "(concept " :suffix ")"
        (print (:name testing.pprinting/holder))
        (pprint-newline :mandatory)
        ((formatter-out "~:<[~;~@{~w~^~:@_~}~;]~:>") (:roles ow.import/holder))
        (pprint-newline :mandatory)
        ((formatter-out "~:<[~;~@{~w~^ ~:_~}~;]~:>") (:super ow.import/holder))
        (pprint-newline :mandatory)
        ((formatter-out "~:<[~;~@{~w~^ ~:_~}~;]~:>") (:restrictions ow.import/holder)))
      (pprint-newline :mandatory)))) 

(defn pprint-mp-property [property]
  (binding [holder property]
    (with-ns 'clojure.contrib.pprint
      (pprint-logical-block :prefix "(property " :suffix ")"
        (print (:name ow.import/holder))
        (pprint-newline :mandatory)
        ((formatter-out "~:<[~;~@{~w~^ ~:_~}~;]~:>") (:restrictions ow.import/holder))
        (pprint-newline :mandatory)
        ((formatter-out "~:<[~;~@{~w~^ ~:_~}~;]~:>") (:super ow.import/holder)))
      (pprint-newline :mandatory)))) 


(defmulti mp-dispatch
  {:arglists '[[obj]]}
  :type)

(defmethod mp-dispatch nil [obj]
  (print obj))

(use-method mp-dispatch :namespace pprint-mp-ns)
(use-method mp-dispatch :concept pprint-mp-concept)
(use-method mp-dispatch :property pprint-mp-property)

;; finish -----------------------------------
                       
(defn iimport
  [{doc-format :from-format gen-folder :to-mp-location mp-ns :mp-domain-ns-generated
    from-file :owl-file-for-import}]
  (let [[folder file-name] (ff-name gen-folder mp-ns)
        mp-ns-sym (symbol mp-ns)
        things (modeling from-file doc-format)]
    (create-file folder (str file-name ".clj")
      (with-out-str 
        (with-pprint-dispatch mp-dispatch 
          (pprint {:type :namespace :name mp-ns-sym :use '(org.uncomplicate.magicpotion 
                                                            org.uncomplicate.magicpotion.predicates 
                                                            ow.restrictions)}))
        (doall (map #(with-pprint-dispatch mp-dispatch 
                       (pprint %))
                 (:dt-properties things)))))))