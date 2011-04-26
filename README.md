OWand
=====



Status
------


###Export
	(ow.core/ow-export-goal)
	
	

###Import
	(ow.core/ow-import-goal)
	
	

Usage
-----

Use as dev-tool (see test/examples)


Use as lein plugin
	
	lein ow-export


	
	lein ow-import

### Configuration

Set ow-config map ([see][owconfig] `ow.core/*ow-config*`) in project.clj (`:ow-config` key) or when calling OWand goals. 
If not specified this default configuration will be used - expected `MP` model is looked for at `src/ow/my_domain.clj`, 
i.e. `ow.my-domain` ns and `examle.owl` file is generated in `ow-export`.
Be sure to state you MP domain namespace (for MP->OWL).

### Installation

Build with [Leiningen][lein]

## License

Copyright (C) 2011

Distributed under the Eclipse Public License, the same as Clojure.

[lein]: https://github.com/technomancy/leiningen
[owconfig]: https://github.com/nevenavv/OWand/blob/master/src/ow/core.clj#L4