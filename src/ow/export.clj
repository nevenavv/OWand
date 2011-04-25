(ns ow.export
  (:use [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.str-utils2 :only [capitalize] :as s]
        [org.uncomplicate.magicpotion.core]
        [org.uncomplicate.magicpotion.m3]
        [org.uncomplicate.magicpotion]
        [ow.engine :as e]
        [ow.IRI]
        [ow.util]
        [ow.restrictions]))

(def n "\n")
(def t "\t")
(def e! "!ENTITY")

;; dt-properties ---------------

(defn get-dt-properties-owl []
  (use 'ow.restrictions);; seems ugly... didn't eval 'use' from file.. 
  (use 'org.uncomplicate.magicpotion.predicates);; TODO fix
  (map (fn [{prop-name :name restrictions :all-restrictions}]
         (let [on-set (set (filter identity (map #(:restriction-on (meta (eval %))) restrictions)))
               _ (assert (<= (count on-set) 1)) ;dt prop restrictions must be for one dt
               on-dt (if-let [n (first on-set)] (name n))
               rest-maps (map #(apply hash-map ((juxt :restriction :restriction-with) (meta (eval %)))) restrictions)
               rests (filter #(not (or (nil? (first (keys %))) ;restrictions fns with no meta are not considered (supported) here
                                       (= :ow.restrictions/not-nil (first (keys %))) ; restrictions fns with :not-nil type are not considered here 
                                       (= :ow.restrictions/type (first (keys %))))) rest-maps) ;restriction for dt is already considered with on-dt
               domain (prop-name e/properties-domain)
               owl-domains (cond (> (count domain) 1) [:rdfs:domain (flatten-1 [:owl:unionOf [{:rdf:parseType "Collection"}] (map #(vec [:owl:Class {:rdf:about (str "#" %)}]) domain)])]
                                 (= (count domain) 1) [:rdfs:domain {:rdf:resource (str "#" (first domain))}])]
           (if (seq rests)
             [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
              (if domain owl-domains [[:comment! "No domain classes"]])
              [:rdfs:range 
               [:rdfs:Datatype
                [:owl:onDatatype {:rdf:resource (str "%26xsd;" on-dt)}
                 (flatten-1 [:owl:withRestrictions [{:rdf:parseType "Collection"}]
                             (flatten-1 (map #(let [r (first (keys %))
                                                    v (second (first %))] 
                                                (cond (= :ow.restrictions/pattern r) [[:xsd:pattern {:xsd:datatype "%26xsd;string"} (str v)]]
                                                      (= :ow.restrictions/min-length r) [[:xsd:minLength {:xsd:datatype "%26xsd;integer"} (str v)]]
                                                      (= :ow.restrictions/max-length r) [[:xsd:maxLength {:xsd:datatype "%26xsd;integer"} (str v)]]
                                                      (= :ow.restrictions/length r) [[:xsd:length {:xsd:datatype "%26xsd;integer"} (str v)]]
                                                      (= :ow.restrictions/before r) [[:xsd:minInclusive {:xsd:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd v)]]
                                                      (= :ow.restrictions/after r) [[:xsd:maxInclusive {:xsd:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd v)]]
                                                      (= :ow.restrictions/lt r) [[:xsd:minInclusive {:xsd:datatype (str "%26xsd;" (name (of-type v)))} (str v)]]
                                                      (= :ow.restrictions/gt r) [[:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type v)))} (str v)]]
                                                      (= :ow.restrictions/s-between r) [[:xsd:minLength {:xsd:datatype "%26xsd;integer"} (str (first v))] 
                                                                                        [:xsd:maxLength {:xsd:datatype "%26xsd;integer"} (str (second v))]]
                                                      (= :ow.restrictions/d-between r) [[:xsd:minInclusive {:xsd:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd (first v))] 
                                                                                        [:xsd:maxInclusive {:xsd:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd (second v))]]
                                                      (= :ow.restrictions/n-between r) [[:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type (first v))))} (str (first v))] 
                                                                                        [:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type (second v))))} (str (second v))]]
                                                      (= :ow.restrictions/has-value r) [[:xsd:pattern {:xsd:datatype "%26xsd;string"} (if (= :ow.restrictions/dateTime (of-type v))
                                                                                                                                        (encoded-datetime-for-xsd v)
                                                                                                                                        (str v))]]
                                                      :else
                                                      [[:comment! (str "Not yet supported restriction: " (name r))]])) 
                                             rests))])]]]]
             (if on-dt [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
                        (if domain owl-domains [[:comment! "No domain classes"]])
                        [:rdfs:range {:rdf:resource (str "%26xsd;" on-dt)}]]
               [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
                (if domain owl-domains [[:comment! "No domain classes"]])]))))
       e/dt-properties))

;; obj-properties --------------------------

(defn get-obj-properties-owl []
  (map (fn [{prop-name :name range-names  :ranges super-names :super}]
         (let [owl-ranges (map #(vec [:rdfs:range {:rdf:resource (str "#" %)}]) range-names)
               owl-sub-properties (map #(vec [:rdfs:subPropertyOf {:rdf:resource (str "#" %)}]) super-names)
               domain (prop-name e/properties-domain)
               owl-domains (cond (> (count domain) 1) [[:rdfs:domain (flatten-1 [:owl:unionOf [{:rdf:parseType "Collection"}] (map #(vec [:owl:Class {:rdf:about (str "#" %)}]) domain)])]]
                                 (= (count domain) 1) [[:rdfs:domain {:rdf:resource (str "#" (first domain))}]])]
           (flatten-1 [:rdf:ObjectProperty [{:rdf:about (str "#" prop-name)}]
                       (if domain owl-domains [[:comment! "No domain classes"]]) 
                       (if (seq range-names) owl-ranges [[:comment! "No range"]])
                       (if (seq super-names) owl-sub-properties [[:comment! "No super properties"]])])))
       e/obj-properties))

;; concepts --------------------------------

(defn get-concepts-owl []
  (map (fn [{nm :name roles :roles super-nms :super restrictions :restrictions disjoints :disjoints roles-construct :roles-construct}]
         (let [disjoints-owl (map #(vec [:owl:disjointWith {:rdf:resource (str "#" %)}]) disjoints)
               supers-owl (map #(vec [:rdfs:subClassOf {:rdf:resource (str "#" %)}]) super-nms)
               roles-owl (map #(vec [:rdfs:subClassof [:owl:Restriction 
                                                       [:owl:onProperty	{:rdf:resource (str "#" (:property-name %))}]
                                                       (cond (and (seq (:ranges (meta (get obj-properties-ranges (:property-name %))))) (= :object (:property-type %))) [:owl:allValuesFrom {:rdf:resource (str "#" (first (:ranges (meta (get obj-properties-ranges (:property-name %))))))}]
                                                             (and (= :datatype (:property-type %)) (seq (:restrictions %))) [:owl:allValuesFrom [:comment! "Datatype def here"]]) 
                                                       (cond (= :min (:cardinality-1 %)) [:owl:minCardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} 1]
                                                             (= :max (:cardinality-1 %)) [:owl:maxCardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} 1]
                                                             (= :eq (:cardinality-1 %)) [:owl:cardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} 1])]]) roles-construct)]
           (flatten-1 [:owl:Class [{:rdf:about (str "#" nm)}]
                       (if (seq disjoints-owl) disjoints-owl [[:comment! "no disjoints"]])
                       (if (seq supers-owl) supers-owl [[:comment! "no super"]])
                       (if (seq roles-owl) roles-owl [[:comment! "no roles"]])])))
       e/concepts))

;; finish -----------------------------------

(defn export
  [ow-config]
  (let [mp-domain (:mp-domain-ns-source ow-config)
        ontology-name (:ontology-name ow-config)]
    (do
      (create-ns mp-domain)
      (doall (map #(ns-unmap *ns* (key %)) (ns-interns mp-domain)))
      (remove-ns mp-domain)
      (use mp-domain :reload)
      (e/assort-things mp-domain)) 
    (create-file (:to-owl-location ow-config) (str ontology-name ".owl")
                 (binding [*prxml-indent* 2]
                   (decode (with-out-str (prxml
                                           [:decl!] n n
                                           [:doctype! (str "rdf:RDF ["
                                                           n t "<" e! " owl \"" owl "\">"
                                                           n t "<" e! " dc \"" dc "\">"
                                                           n t "<" e! " xsd \"" xsd "\">"
                                                           n t "<" e! " rdfs \"" rdfs "\">"
                                                           n t "<" e! " rdf \"" rdf "\">"
                                                           n t "<" e! " "(ont-name-for-pr ow-config)" \"" (ont-full-ns ow-config) "#\">")
                                            n "]"] n
                                           [:rdf:RDF {:xmlns (str (ont-full-ns ow-config) "#")
                                                      :xml:base (ont-full-ns ow-config)
                                                      (keyword (str "xmlns:" (ont-name-for-pr ow-config))) (str (ont-full-ns ow-config) "#")
                                                      :xmlns:owl owl
                                                      :xmlns:dc dc
                                                      :xmlns:xsd xsd
                                                      :xmlns:rdfs rdfs}
                                            [:owl:Ontology {:rdf:about (ont-full-ns ow-config)}
                                             [:dc:title (s/capitalize ontology-name)]
                                             [:dc:description "#FIXME Ontology Description#"]]
                                            n n
                                            [:comment! "### Datatype properties ###"]
                                            n
                                            (interpose n (get-dt-properties-owl))
                                            n n
                                            [:comment! "### Object properties ###"]
                                            n
                                            (interpose n (get-obj-properties-owl))
                                            n n
                                            [:comment! "### Classes ###"]
                                            n n
                                            (interpose n (get-concepts-owl))
                                            n
                                            ])))))))
