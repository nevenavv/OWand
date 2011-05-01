(ns ow.export
  (:use [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.str-utils2 :only [capitalize] :as s]
        [org.uncomplicate.magicpotion.core]
        [org.uncomplicate.magicpotion.m3]
        [org.uncomplicate.magicpotion]
        [ow.engine :as e]
        [ow.IRI]
        [ow.util]
        [ow.restrictions :as r]))

(def n "\n")
(def t "\t")
(def e! "!ENTITY")

;; dt-properties ---------------

(defn datatype-owl
  [on-dt rests]
  (vec [:rdfs:Datatype
        [:owl:onDatatype {:rdf:resource (str "%26xsd;" on-dt)}]
        (loop [rs rests
               rez [:owl:withRestrictions {:rdf:parseType "Collection"}]]
          (if-let [n (first rs)]
            (recur (rest rs)
                   (let [r (first (keys n))
                         v (second (first n))
                         to-add (cond (= ::r/pattern r) [[:xsd:pattern {:rdf:datatype "%26xsd;string"} (str v)]]
                                      (= ::r/min-length r) [[:xsd:minLength {:rdf:datatype "%26xsd;integer"} (str v)]]
                                      (= ::r/max-length r) [[:xsd:maxLength {:rdf:datatype "%26xsd;integer"} (str v)]]
                                      (= ::r/length r) [[:xsd:length {:rdf:datatype "%26xsd;integer"} (str v)]]
                                      (= ::r/before r) [[:xsd:minInclusive {:rdf:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd v)]]
                                      (= ::r/after r) [[:xsd:maxInclusive {:rdf:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd v)]]
                                      (= ::r/lt r) [[:xsd:minInclusive {:rdf:datatype (str "%26xsd;" (name (of-type v)))} (str v)]]
                                      (= ::r/gt r) [[:xsd:maxInclusive {:rdf:datatype (str "%26xsd;" (name (of-type v)))} (str v)]]
                                      (= ::r/s-between r) [[:xsd:minLength {:rdf:datatype "%26xsd;integer"} (str (first v))] 
                                                           [:xsd:maxLength {:rdf:datatype "%26xsd;integer"} (str (second v))]]
                                      (= ::r/d-between r) [[:xsd:minInclusive {:rdf:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd (first v))] 
                                                           [:xsd:maxInclusive {:rdf:datatype "%26xsd;dateTime"} (encoded-datetime-for-xsd (second v))]]
                                      (= ::r/n-between r) [[:xsd:maxInclusive {:rdf:datatype (str "%26xsd;" (name (of-type (first v))))} (str (first v))] 
                                                           [:xsd:maxInclusive {:rdf:datatype (str "%26xsd;" (name (of-type (second v))))} (str (second v))]]
                                      (= ::r/has-value r) [[:xsd:pattern {:rdf:datatype "%26xsd;string"} (if (= :ow.restrictions/dateTime (of-type v))
                                                                                                           (encoded-datetime-for-xsd v)
                                                                                                           (str v))]]
                                      :else
                                      [[:comment! (str "Not yet supported restriction: " (name r))]])]
                     (apply conj rez (map #(conj [:rdf:Description] %) to-add))))
            rez))]))

(defn get-dt-properties-owl []
  (map (fn [{prop-name :name restrictions :restrictions super-names :super}]
         (let [on-set (set (filter identity (map #(:restriction-on (meta (eval %))) restrictions)))
               _ (assert (<= (count on-set) 1)) ;dt prop restrictions must be for one dt
               on-dt (if-let [n (first on-set)] (name n))
               rest-maps (map #(apply hash-map ((juxt :restriction :restriction-with) (meta (eval %)))) restrictions)
               rests (filter #(not (or (nil? (first (keys %))) ;restrictions fns with no meta are not considered (supported) here
                                       (= :ow.restrictions/not-nil (first (keys %))) ; restrictions fns with :not-nil type are not considered here 
                                       (= :ow.restrictions/type (first (keys %))))) rest-maps) ;restriction for dt is already considered with on-dt
               domain (prop-name e/properties-domain)
               owl-domains (cond (> (count domain) 1) [:rdfs:domain [:owl:Class (flatten-1 [:owl:unionOf [{:rdf:parseType "Collection"}] (map #(vec [:owl:Class {:rdf:about (str "#" %)}]) domain)])]]
                                 (= (count domain) 1) [:rdfs:domain {:rdf:resource (str "#" (first domain))}])
               owl-sub-properties (map #(vec [:rdfs:subPropertyOf {:rdf:resource (str "#" %)}]) super-names)]
           [:owl:DatatypeProperty {:rdf:about (str "#" prop-name)}
             (if (seq super-names) owl-sub-properties [:comment! "No super properties"])
             (if domain owl-domains [:comment! "No domain classes"])
             (if (seq rests)
               [:rdfs:range (datatype-owl on-dt rests)]
               (if on-dt [:rdfs:range {:rdf:resource (str "%26xsd;" on-dt)}] [:comment! "No range"]))]))
    e/dt-properties))

;; obj-properties --------------------------

(defn get-obj-properties-owl []
  (map (fn [{prop-name :name range-names  :ranges super-names :super}]
         (let [owl-ranges (map #(vec [:rdfs:range {:rdf:resource (str "#" %)}]) range-names)
               owl-sub-properties (map #(vec [:rdfs:subPropertyOf {:rdf:resource (str "#" %)}]) super-names)
               domain (prop-name e/properties-domain)
               owl-domains (cond (> (count domain) 1) [:rdfs:domain [:owl:Class (flatten-1 [:owl:unionOf [{:rdf:parseType "Collection"}] (map #(vec [:owl:Class {:rdf:about (str "#" %)}]) domain)])]]
                                 (= (count domain) 1) [:rdfs:domain {:rdf:resource (str "#" (first domain))}])]
           [:owl:ObjectProperty {:rdf:about (str "#" prop-name)}
            (if (seq super-names) owl-sub-properties [:comment! "No super properties"])           
            (if domain owl-domains [:comment! "No domain classes"]) 
            (if (seq range-names) owl-ranges [:comment! "No range"])]))
       e/obj-properties))

;; concepts --------------------------------

(defn get-concepts-owl []
  (map (fn [{nm :name roles :roles super-nms :super restrictions :restrictions disjoints :disjoints roles-construct :roles-construct}]
         (let [disjoints-owl (map #(vec [:owl:disjointWith {:rdf:resource (str "#" %)}]) disjoints)
               supers-owl (map #(vec [:rdfs:subClassOf {:rdf:resource (str "#" %)}]) super-nms)
               roles-owl (map #(vec [:rdfs:subClassOf [:owl:Restriction 
                                                       [:owl:onProperty	{:rdf:resource (str "#" (:property-name %))}]
                                                       (cond (and (seq (:ranges (meta (get obj-properties-ranges (:property-name %))))) (= :object (:property-type %))) 
                                                               [:owl:allValuesFrom {:rdf:resource (str "#" (first (:ranges (meta (get obj-properties-ranges (:property-name %))))))}]
                                                             (and (= :datatype (:property-type %)) (seq (:restrictions %))) 
                                                               (let [on-set (set (filter identity (map (fn [r] (:restriction-on (meta (eval r)))) (:restrictions %))))
                                                                     _ (assert (<= (count on-set) 1)) ;dt prop restrictions must be for one dt
                                                                     on-dt (if-let [n (first on-set)] (name n))
                                                                     rest-maps (map (fn [r] (apply hash-map ((juxt :restriction :restriction-with) (meta (eval r))))) (:restrictions %))
                                                                     rests (filter (fn [r] (not (or (nil? (first (keys r))) ;restrictions fns with no meta are not considered (supported) here
                                                                                                    (= :ow.restrictions/not-nil (first (keys r))) ; restrictions fns with :not-nil type are not considered here 
                                                                                                    (= :ow.restrictions/type (first (keys r)))))) rest-maps)] ;restriction for dt is already considered with on-dt]
                                                                 (if (seq on-set) [:owl:allValuesFrom (datatype-owl on-dt rests)] [:comment! "Unsupported datatype restriciton"]))) 
                                                       (if-let [c (:min (:cardinality %))] [:owl:minCardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} (str c)])
                                                       (if-let [c (:max (:cardinality %))] [:owl:maxCardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} (str c)])
                                                       (if-let [c (:eq (:cardinality %))] [:owl:cardinality {:rdf:datatype "%26xsd;nonNegativeInteger"} (str c)])]]) roles-construct)]
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
      (use 'ow.restrictions);; seems ugly... didn't eval 'use' from model file.. 
      (use 'org.uncomplicate.magicpotion.predicates);; TODO fix
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
                                                      :xmlns:rdf rdf
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
                                            n
                                            (interpose n (get-concepts-owl))
                                            n
                                            ])))))))
