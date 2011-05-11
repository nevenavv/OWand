(ns ow.import
  (:use [plaza.rdf.core]
        [plaza.rdf.implementations.jena]
        [plaza.rdf.sparql]
        [plaza.rdf.predicates]
        [clojure.contrib.pprint]
        [clojure.contrib.with-ns]
        [ow.util]))

(def concepts) 
(def dt-properties) 
(def obj-properties)

(init-jena-framework)
(register-rdf-ns :owl "http://www.w3.org/2002/07/owl#")
;(register-rdf-ns :rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#")

(defn modeling
  [file doc-format domain-ns ontname]
  (let [_m (document-to-model  (java.io.FileInputStream. file) doc-format)
        t (model-to-triples *rdf-model*)
        domainns (str (ont-full-ns {:ont-root-domain-ns domain-ns :ontology-name ontname}) "#")
        dt-properties (vals (reduce  #(let [dtpname ((comp qname-local :?dtp) %2)]
                                        (if-let [super (:?dtpsuper %2)]
                                          (assoc %1 dtpname {:name dtpname :type :property :super (conj (:super (get %1 dtpname)) (qname-local super))})
                                          (assoc %1 dtpname {:name dtpname :type :property :super (:super (get %1 dtpname))})))
                              {} (model-query *rdf-model* (defquery
                                                            (query-set-type :select)
                                                            (query-set-vars [:?dtp :?dtpsuper])
                                                            (query-set-pattern
                                                              (make-pattern [[:?dtp [:rdf :type] [:owl :DatatypeProperty]]
                                                                             (optional [:?dtp [:rdfs :subPropertyOf] :?dtpsuper])]))))))
        obj-properties (vals (reduce  #(let [opname ((comp qname-local :?op) %2)
                                             rng (myb-str (if (and (:?c %2) (= domainns ((comp qname-prefix :?c) %2))) ((comp qname-local :?c) %2)) "?")
                                             super (if (and (:?opsuper %2) (= domainns ((comp qname-prefix :?opsuper) %2))) ((comp qname-local :?opsuper) %2))]
                                          (assoc %1 opname {:name opname :type :property 
                                                            :super (myb-conj (:super (get %1 opname)) super) 
                                                            :restrictions (myb-conj (:restrictions (get %1 opname)) rng)}))
                              {} (model-query *rdf-model* (defquery
                                                            (query-set-type :select)
                                                            (query-set-vars [:?op :?opsuper :?c])
                                                            (query-set-pattern
                                                              (make-pattern [[:?op [:rdf :type] [:owl :ObjectProperty]]
                                                                             (optional [:?op [:rdfs :subPropertyOf] :?opsuper])
                                                                             (optional [:?op [:rdfs :range] :?c])]))))))
         concepts (vals (reduce  #(let [cname ((comp qname-local :?c) %2)
                                        ok-cname (= domainns ((comp qname-prefix :?c) %2))
                                        super (if (and (:?csuper %2) (= domainns ((comp qname-prefix :?csuper) %2))) ((comp qname-local :?csuper) %2))]
                                    (if ok-cname    
                                      (assoc %1 cname {:name cname :type :concept :super (myb-conj (:super (get %1 cname)) super)})
                                      %1))
                              {} (model-query *rdf-model* (defquery
                                                            (query-set-type :select)
                                                            (query-set-vars [:?c :?csuper])
                                                            (query-set-pattern
                                                              (make-pattern [[:?c [:rdf :type] [:owl :Class]]
                                                                             (optional [:?c [:rdfs :subClassOf] :?csuper])]))))))]
    
    (intern 'ow.import 'concepts concepts)
    (intern 'ow.import 'dt-properties dt-properties)
    (intern 'ow.import 'obj-properties obj-properties)))

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
        (print (:name ow.import/holder))
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
    from-file :owl-file-for-import domain-ns :ont-root-domain-ns ontname :ontology-name}]
  (let [[folder file-name] (ff-name gen-folder mp-ns)
        mp-ns-sym (symbol mp-ns)
        things (modeling from-file doc-format domain-ns ontname)]
    (create-file folder (str file-name ".clj")
      (with-out-str 
        (with-pprint-dispatch mp-dispatch 
          (pprint {:type :namespace :name mp-ns-sym :use '(org.uncomplicate.magicpotion 
                                                            org.uncomplicate.magicpotion.predicates 
                                                            ow.restrictions)}))
        (doall (map #(with-pprint-dispatch mp-dispatch 
                       (pprint %))
                 (concat dt-properties obj-properties concepts)))))))