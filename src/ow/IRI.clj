(ns ow.IRI
  (:require [clojure.contrib.str-utils2 :only [split contains?] :as s]))

(def owl "http://www.w3.org/2002/07/owl#")
(def dc "http://purl.org/dc/elements/1.1/")
(def xsd "http://www.w3.org/2001/XMLSchema#")
(def rdfs "http://www.w3.org/2000/01/rdf-schema#")
(def rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#")

(defn iri-expand
  [iri-keyword]
  (let [val (name iri-keyword)
       [pname local] (s/split val #":")
       pnamesym (symbol pname)]
    (if-not (resolve pnamesym) (throw (Exception. "Namespace not defined.")))
    (str (eval pnamesym) local)))
    
(comment
(defmacro iri
  [compact]
  (let [val (name compact)
        _ (assert (s/contains? val ":"))
        [pname local] (s/split val #":")
        pnamesym (symbol pname)]
    `(do 
       (if-not (resolve '~pnamesym) (throw (Exception. "Namespace not defined.")))
       (intern *ns* '~compact (str ~pnamesym ~local)))))

(macroexpand '(iri abc:ahaha))

(map iri [
  ;OWL 2 RDF-Based Vocabulary
  owl:AllDifferent
	owl:AllDisjointClasses
	owl:AllDisjointProperties
	owl:allValuesFrom
	owl:annotatedProperty
	owl:annotatedSource
	owl:annotatedTarget
	owl:Annotation
	owl:AnnotationProperty
	owl:assertionProperty
	owl:AsymmetricProperty
	owl:Axiom
	owl:backwardCompatibleWith
	owl:bottomDataProperty
	owl:bottomObjectProperty
	owl:cardinality
	owl:Class
	owl:complementOf
	owl:DataRange
	owl:datatypeComplementOf
	owl:DatatypeProperty
	owl:deprecated
	owl:DeprecatedClass
	owl:DeprecatedProperty
	owl:differentFrom
	owl:disjointUnionOf
	owl:disjointWith
	owl:distinctMembers
	owl:equivalentClass
	owl:equivalentProperty
	owl:FunctionalProperty
	owl:hasKey
	owl:hasSelf
	owl:hasValue
	owl:imports
	owl:incompatibleWith
	owl:intersectionOf
	owl:InverseFunctionalProperty
	owl:inverseOf
	owl:IrreflexiveProperty
	owl:maxCardinality
	owl:maxQualifiedCardinality
	owl:members
	owl:minCardinality
	owl:minQualifiedCardinality
	owl:NamedIndividual
	owl:NegativePropertyAssertion
	owl:Nothing
	owl:ObjectProperty
	owl:onClass
	owl:onDataRange
	owl:onDatatype
	owl:oneOf
	owl:onProperty
	owl:onProperties
	owl:Ontology
	owl:OntologyProperty
	owl:priorVersion
	owl:propertyChainAxiom
	owl:propertyDisjointWith
	owl:qualifiedCardinality
	owl:ReflexiveProperty
	owl:Restriction
	owl:sameAs
	owl:someValuesFrom
	owl:sourceIndividual
	owl:SymmetricProperty
	owl:targetIndividual
	owl:targetValue
	owl:Thing
	owl:topDataProperty
	owl:topObjectProperty
	owl:TransitiveProperty
	owl:unionOf
	owl:versionInfo
	owl:versionIRI
	owl:withRestrictions
  ;RDFS vocabulary
  rdfs:domain
	rdfs:range
	rdfs:Resource
	rdfs:Literal
	rdfs:Datatype
	rdfs:Class
	rdfs:subClassOf
	rdfs:subPropertyOf
	rdfs:member
	rdfs:Container
	rdfs:ContainerMembershipProperty
	rdfs:comment
	rdfs:seeAlso
	rdfs:isDefinedBy
	rdfs:label
  ;RDF classes
  rdfs:Resource
	rdfs:Literal
	rdf:XMLLiteral
	rdfs:Class
	rdf:Property
	rdfs:Datatype
	rdf:Statement
	rdf:Bag
	rdf:Seq
	rdf:Alt
	rdfs:Container
	rdfs:ContainerMembershipProperty
	rdf:List
  ;RDF properties
  rdf:type
	rdfs:subClassOf
	rdfs:subPropertyOf
	rdfs:domain
	rdfs:range
	rdfs:label
	rdfs:comment
	rdfs:member
	rdf:first
	rdf:rest
	rdf:value
	rdf:subject
	rdf:predicate
	rdf:object
  ;datatypes of the OWL 2 RDF-Based Semantics
  xsd:anyURI
	xsd:base64Binary
	xsd:boolean
	xsd:byte
	xsd:dateTime
	xsd:dateTimeStamp
	xsd:decimal
	xsd:double
	xsd:float
	xsd:hexBinary
	xsd:int
	xsd:integer
	xsd:language
	xsd:long
	xsd:Name
	xsd:NCName
	xsd:negativeInteger
	xsd:NMTOKEN
	xsd:nonNegativeInteger
	xsd:nonPositiveInteger
	xsd:normalizedString
	rdf:PlainLiteral
	xsd:positiveInteger
	owl:rational
	owl:real
	xsd:short
	xsd:string
	xsd:token
	xsd:unsignedByte
	xsd:unsignedInt
	xsd:unsignedLong
	xsd:unsignedShort
	rdf:XMLLiteral
  ;Facets of the OWL 2 RDF-Based Semantics
  rdf:langRange
	xsd:length
	xsd:maxExclusive
	xsd:maxInclusive
	xsd:maxLength
	xsd:minExclusive
	xsd:minInclusive
	xsd:minLength
	xsd:pattern
])
;end of comment
)