(ns ow.export
  (:use [clojure.contrib.prxml :only [prxml *prxml-indent*]]
        [clojure.contrib.str-utils2 :only [capitalize] :as s]
        [clojure.contrib.ns-utils :as nsu]
        [clojure.contrib.seq :only [separate]]
        [org.uncomplicate.magicpotion.core]
        [org.uncomplicate.magicpotion.m3]
        [org.uncomplicate.magicpotion]
        [ow.engine]
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
                                                  (= :ow.restrictions/n-between r) [[:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type (first v))))} (str (first v))] 
                                                                                    [:xsd:maxInclusive {:xsd:datatype (str "%26xsd;" (name (of-type (second v))))} (str (second v))]]
                                                  (= :ow.restrictions/has-value r) [[:xsd:pattern {:xsd:datatype "%26xsd;string"} (if (= :ow.restrictions/dateTime (of-type v))
                                                                                                                                    (encoded-datetime-for-xsd v)
                                                                                                                                    (str v))]]
                                                  :else
                                                  [[:comment! (str "Not yet supported restriction: " (name r))]])) 
                                          rests))])]]]]
             (if on-dt [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}
                        [:rdfs:range {:rdf:resource (str "%26xsd;" on-dt)}]]
               [:rdf:DatatypeProperty {:rdf:about (str "#" prop-name)}]))))
    dt-properties))

;; obj-properties --------------------------

(defn get-obj-properties-owl []
  (map (fn [{prop-name :name range-names  :ranges super-names :super}]
          (let [owl-ranges (map #(vec [:rdfs:range {:rdf:resource (str "#" %)}]) range-names)
                owl-sub-properties (map #(vec [:rdfs:subPropertyOf {:rdf:resource (str "#" %)}]) super-names)]
          (flatten-1 [:rdf:ObjectProperty [{:rdf:about (str "#" prop-name)}]
                      (if (seq range-names) owl-ranges)
                      (if (seq super-names) owl-sub-properties)])))
    obj-properties))

;; concepts --------------------------------

(defn get-concepts-owl []
  (map (fn [{nm :name roles :roles super-nms :super restrictions :restrictions}]
         (let [number (+ (count roles) (count super-nms))
               intersec (> number 1)]
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
    concepts))
  
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
      (assort-things mp-domain)) 
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
  