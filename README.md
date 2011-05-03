OWand
=====
[Magic Potion][mp] <-> [OWL][owl] Transformation Tool
Status
------


###Export

Not yet processed:
	
* user defined restrictions (!?)
* instances
* `set-restrictions` on `val*>` and `ref*>` roles [except cardinality]
* concept's restrictions [except property key for not-nil restriction of `val>` and `ref>` roles]
* some restrictions from `ow.restrictions`, e.g. `has-value`, `in`

Generated `OWL` file is [RDF Valid][rdfv] and [Protégé][prot] sucessfully parsed.

Generated `OWL` model is in `RDF/XML` syntax and can be easily transformed to other `OWL`(`RDF`) syntax formats using available tools.

###Import
`Nil`
	
Details
----

`MP` -> `OWL` transformation (export) is not  `1-to-1` transformation due to `OWL`'s limited expresiveness and `MP`'s rich expresiveness. Details are disscussed further below.

###Transformation rationale/rules


####Restriction functions
**Case 1** Restrictions using `MP` `predicates` and `ow.restrictions`

Besides `before?` and `after?` (as stated below), predicates from `MP` `predicates` are easily transformed to OWL counterparts (due to no obvious advantage and need for additional logic for *manual* processing of this predicates, now they are [hooked][hook] to appropriate restriction functions from `ow.restrictions`

**Case 2** Restrictions as user defined predicates

Highly unlikely to cover those as it could be **anything**. With that, when defining model user is encouraged to use already defined predicates/restriction functions from `MP`/`predicates` and `ow.restrictions`.

####Disjoint classes
Currently all Classes on same level of hierarchy are disjointed from one another.

####Other

- `OWL` temporal logic support - how to represent `before?` and `after?` restriction
- `restrictions` on `MP` `concept` construct may be used to define all kinds of restrictions, both on single properties (instead of `role` `restrictions` for example) and whole concept as group of properties (for example predicate as `some`, `any` restriction on some concpet properties), relation between properties (for this case `OWL` support is very limited)... where is the limit?

---

####Presumptions
- no forward declaration of model elements (no use of `declare`, all elements that the one depends on are defined before that one)

####Other TODOs
- logging, e.g. reporting transformation results
- maybe some logical checks/validaton

Usage
-----

Configuration is defined with following map (`<ow-config>`)

<table>
<tr><td><b>key</b></td><td><b>val-type</b></td><td><b>default</b></td><td><b>description</b></td><td><b>now used</b></td></tr>

<tr><td>:from-format</td><td>keyword</td><td>:rdf/xml</td><td>format of input owl file</td><td>no</td></tr>
<tr><td>:to-format</td><td>keyword</td><td>:rdf/xml</td><td>format of output owl file</td><td>no</td></tr>
<tr><td>:ontology-name</td><td>string</td><td>"example"</td><td>ontology name</td><td>yes</td></tr>
<tr><td>:from-owl-location</td><td>string</td><td>"/"</td><td>location of owl files for import</td><td>no</td></tr>
<tr><td>:to-owl-location</td><td>string</td><td>"ow-export/"</td><td>location where owl generated file will be set</td><td>yes</td></tr>
<tr><td>:mp-domain-ns-generated</td><td>string</td><td>"domain"</td><td>ns of generated magic potion model </td><td>no</td></tr>
<tr><td>:mp-domain-ns-source</td><td>symbol</td><td>'ow.my-domain</td><td>source ns of magic potion model; currently looking in <i><b>src/</b></i> folder as root</td><td>yes</td></tr>
<tr><td>:ont-root-domain-ns</td><td>string</td><td>"http://example.org/ontologies/"</td><td>owl domain namespace for this ontology model</td><td>yes</td></tr>
</table>

Available util functions: `ow.core/owc-update-default` and `ow.core/owc-reset-to-default`.

Interactive changes on model will be automatically updated on next call of desired goal (no need to explicitely reload model's ns).

###Try in REPL

    lein deps, compile, repl

    =>(use 'ow.core)
    =>(ow-export-goal)

###Use s dev-tool

####Export
	(ow.core/ow-export-goal)
	(ow.core/ow-export-goal <ow-config>)

####Import
	
	(ow.core/ow-import-goal)
	(ow.core/ow-import-goal <ow-config>)

If `<ow-config>` is supplied, given pairs will override default.

###Use as Leiningen plugin

Build project and in `:dev-dependency` in your `project.clj` add:

    [ow "0.0.1-SNAPSHOT"]

If supplied, plugin will override default configuration pairs with ones on `:ow-config` in `project.clj`   
####Export goal

	lein ow-export

####Import goal

	lein ow-import

## Installation

Build jar with [Leiningen][lein]

*Magic Potion* must be available on classpath. 
<br/>

-----------
#### License

~~Copyright (C) Nevena Vidojević 2011~~

Distributed under the Eclipse Public License, the same as Clojure.

[lein]: https://github.com/technomancy/leiningen
[owconfig]: https://github.com/nevenavv/OWand/blob/master/src/ow/core.clj#L4
[rdfv]: http://www.w3.org/RDF/Validator
[prot]: http://protege.stanford.edu/
[mp]: http://www.uncomplicate.org/magicpotion
[owl]: http://www.w3.org/2007/OWL/wiki/OWL_Working_Group
[hook]: https://github.com/nevenavv/OWand/blob/master/src/ow/restrictions.clj#L198