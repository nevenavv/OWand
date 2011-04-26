OWand
=====

Status
------


###Export

Not yet processed:
	
* `set-restrictions` on `val*>` and `ref*>` roles
* concept's `restrictions`
* some restrictions from `ow.restrictions`, e.g. `has-value` and `in`
* instances

Generated `OWL` file is [RDF Valid][rdfv] and [Protégé][prot] sucessfully parsed.

###Import
Nil
	
Rationale
----

`MP` -> `OWL` transformation (export) is not  `1-to-1` transformation due to `OWL`'s limited expresivness and `MP`'s rich expresiveness. Details are disscussed further below.
###Transformation compatibilites

Usage
-----

####Use as dev-tool
	(ow.core/ow-export-goal)
and
	
	(ow.core/ow-import-goal)
####Use as lein plugin
	
	lein ow-export
and

	lein ow-import

### Configuration

Set ow-config map ([see][owconfig] `ow.core/*ow-config*`) in project.clj (`:ow-config` key) or when calling OWand goals. If not specified this default configuration will be used - expected `MP` model is looked for at `src/ow/my_domain.clj`, i.e. `ow.my-domain` ns and `examle.owl` file is generated in `ow-export`.

### Installation

Build with [Leiningen][lein]

#### License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.

[lein]: https://github.com/technomancy/leiningen
[owconfig]: https://github.com/nevenavv/OWand/blob/master/src/ow/core.clj#L4
[rdfv]: http://www.w3.org/RDF/Validator
[prot]: http://protege.stanford.edu/