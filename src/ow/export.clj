(ns ow.export
  (:use [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.str-utils2 :only [capitalize] :as s]
        [clojure.contrib.ns-utils :as nsu]
        [clojure.contrib.seq :only [separate]]
        [org.uncomplicate.magicpotion.core]
        [org.uncomplicate.magicpotion.m3]
        [org.uncomplicate.magicpotion]
        [ow.IRI]
        [ow.util]
        [ow.restrictions]))

(def n "\n")
(def t "\t")
(def e! "!ENTITY")


;; properties ---------------
(defn- get-all-restrictions
  "Gets all restrictions for property (including all ancestor retrictions)"
  [mp-property]
  (let [prop-desc (:org.uncomplicate.magicpotion.m3/def (meta mp-property))]
    (deep :restrictions prop-desc)))

(defn mp-properties-grouped
  "Returns [(object-props) (datatype-props)]"
  [domain-ns]
  (let [props (filter #(property? (var-get (ns-resolve domain-ns %))) (nsu/ns-vars domain-ns))]
     (separate (fn [prop] 
                 (some #(= :object (:restriction (meta %)))
                       (get-all-restrictions (eval prop))))
               props)))

(defn dt-property-restrictions-pair
  [mp-property]
  {:pre [(property? mp-property)]}
  (let [desc (:org.uncomplicate.magicpotion.m3/def (meta mp-property))
        nm (:name desc)
        restrictions (deep :restrictions desc)]
    [(name nm) restrictions]))

(defn all-dt-props-pairs
  [ns]
  (map #(dt-property-restrictions-pair (var-get (ns-resolve ns %))) (second (mp-properties-grouped ns))))

(defn obj-property-set
  [mp-property]
  {:pre [(property? mp-property)]}
  (let [desc (:org.uncomplicate.magicpotion.m3/def (meta mp-property))
        nm (:name desc)
        top-restrictions (:restrictions desc)
        super-nms (if-let [sup (seq (:super desc))] (map #(name (:name %)) sup))]
    [(name nm) top-restrictions super-nms]))
  
(defn all-obj-props-sets
  [ns]
  (map #(obj-property-set (var-get (ns-resolve ns %))) (first (mp-properties-grouped ns))))

  
(defn dt-prop-cons
  [[prop-name restrictions]]
  (let [on-set (set (filter identity (map #(:restriction-on (meta %)) restrictions)))
        _ (assert (<= (count on-set) 1)) ;dt prop restrictions must be for one dt
        on-dt (if-let [n (first on-set)] (name n))
        rest-maps (map #(apply hash-map ((juxt :restriction :restriction-with) (meta %))) restrictions)
        rests (filter #(not (or (nil? (first (keys %))) ;restrictions fns with no meta are not considered (supported) here
                                (= :ow.restrictions/not-nil (first (keys %))) ; restrictions fns with :not-nil type are not considered here 
                                (= :ow.restrictions/type (first (keys %))))) rest-maps)] ;restriction for dt is already considered with on-dt
    (if (seq rests)
      [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
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
                                                (= :ow.restrictions/n-between r) [[:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type v)))} (str (first v))] 
                                                                                  [:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type v)))} (str (second v))]]
                                                (= :ow.restrictions/has-value r) [[:xsd:pattern {:xsd:datatype "%26xsd;string"} (if (= :ow.restrictions/dateTime (of-type v))
                                                                                                                                                          (encoded-datetime-for-xsd v)
                                                                                                                                                          (str v))]]
                                                :else
                                                [[:comment! (str "Not yet supported restriction: " (name r))]])) rests))])]]]]
       (if on-dt [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
                  [:rdfs:range {:rdf:resource (str "%26xsd;" on-dt)}]]
                 [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}]))))


(defn obj-prop-cons
  [[prop-name top-restrictions super-names]]
  (let [range-names (filter identity (map #(if (= :object (:restriction (meta %))) (trim-ending-str (name (:restriction-with (meta %))) "?")) top-restrictions))]
    (flatten-1 [:rdf:ObjectProperty [{:rdf:about (str "#" prop-name)}]
                                 (if (seq range-names) (map #(vec [:rdfs:range {:rdf:resource (str "#" %)}]) range-names))
                                 (if (seq super-names) (map #(vec [:rdfs:subPropertyOf {:rdf:resource (str "#" %)}]) super-names))])))

;; concepts --------------------------------
(defn mp-concepts
  [domain-ns]
  (let [concepts (filter #(concept? (var-get (ns-resolve domain-ns %))) (nsu/ns-vars domain-ns))]
     concepts))
  
(defn concept-set
  [mp-concept]
  {:pre [(concept? mp-concept)]}
  (let [desc (:org.uncomplicate.magicpotion.m3/def (meta mp-concept))
        nm (:name desc)
        roles (:roles desc)
        super-nms (if-let [sup (seq (:super desc))] (map #(name (:name %)) sup))
        _restrictions (:restrictions desc)]
    [(name nm) roles super-nms _restrictions]))

(defn all-concept-sets
  [ns]
  (map #(concept-set (var-get (ns-resolve ns %))) (mp-concepts ns)))

(defn concept-cons
  [[nm roles super-nms restrictions]]
  (let [number (+ (count roles) (count super-nms))
        intersec (if (> number 1) true false)]
    [:owl:Class {:rdf:about (str "#" nm)}
     (if (> number 0)
       [:rdfs:subClassOf
        (if intersec
          [:owl:Class
           [:owl:intersectionOf {:rdf:parseType "Collection"}
            [:comment! "Coming soon"]
            ;loop over roles and super-nms
            ]]
          (cond (= 1 (count super-nms)) [:rdf:Description {:rdf:about (str "#" (first super-nms))}]
                (= 1 (count roles)) [:owl:Restriction [:comment! "Coming soon"]]))]
       [:comment! "no roles, no subclasses, but don't want nil"])]))
  

;; finish -----------------------------------
(defn export
  [ow-config]
  (do
    (create-ns (:mp-domain-package-source ow-config))
	  (doall (map #(ns-unmap *ns* (key %))  (ns-interns (:mp-domain-package-source ow-config))))
	  (remove-ns (:mp-domain-package-source ow-config))
	  (use (:mp-domain-package-source ow-config) :reload))
	(create-file (:to-owl-location ow-config) (str (:ontology-name ow-config) ".owl")
	  (binding [*prxml-indent* 2]
	    (decode (with-out-str (prxml
	                            [:decl!] n n
	                            [:doctype! (str "rdf:RDF ["
	                                         n t "<" e! " owl \"" owl "\">"
	                                         n t "<" e! " dc \"" dc "\">"
	                                         n t "<" e! " xsd \"" xsd "\">"
	                                         n t "<" e! " rdfs \"" rdfs "\">"
	                                         n t "<" e! " rdf \"" rdf "\">"
	                                         n t "<" e! " "(ont-name ow-config)" \"" (ont-full-ns ow-config) "#\">")
	                             n "]"] n
	                            [:rdf:RDF {:xmlns (str (ont-full-ns ow-config) "#")
	                                       :xml:base (ont-full-ns ow-config)
	                                       (keyword (str "xmlns:" (ont-name ow-config))) (str (ont-full-ns ow-config) "#")
	                                       :xmlns:owl owl
	                                       :xmlns:dc dc
	                                       :xmlns:xsd xsd
	                                       :xmlns:rdfs rdfs}
	                             [:owl:Ontology {:rdf:about (ont-full-ns ow-config)}
	                              [:dc:title (s/capitalize (:ontology-name ow-config))]
	                              [:dc:description "#FIXME Ontology Description#"]]
	                             n n
	                             [:comment! "### Datatype properties ###"]
	                             n
	                             (interpose n (map dt-prop-cons (all-dt-props-pairs (:mp-domain-package-source ow-config))))
	                             n n
	                             [:comment! "### Object properties ###"]
	                             n
	                             (interpose n (map obj-prop-cons (all-obj-props-sets (:mp-domain-package-source ow-config))))
	                             n n
	                             [:comment! "### Classes ###"]
	                             n n
                              (interpose n (map concept-cons (all-concept-sets (:mp-domain-package-source ow-config))))
                              n
	                             ]))))))
