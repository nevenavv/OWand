(ns ow.test.export-test
  (:use [ow.export])
  (:use [clojure.test]))

[[:rdfs:domain (flatten-1 [:owl:unionOf {:rdf:parseType "Collection"} 
														(map #(vec [:owl:Class {:rdf:about (str "#" %)}]) ['a 'b 'c])])]]
;; TODO

(comment
(def d 'ow.test.examples.my-domain)
(def m (map #(var-get (ns-resolve d %)) (nsu/ns-vars d)))
(map #(let [x %] (try (var-get (val x)) 
                   (catch Exception e (deref (val x)))))
  (ns-interns d))

(map #(instance? clojure.lang.Ref %) m)
(map #(type %) m)s
(ns-interns d)
(map #(meta (var-get (ns-resolve d %))) (nsu/ns-vars d))
(map #(ns-unmap d (key %))  (ns-interns d))
(ns-refers 'ow.core)
(create-ns d)
(map #(ns-unmap 'ow.core (key %))  (ns-interns 'ow.test.examples.my-domain))
(remove-ns 'ow.teset.examples.my-domain)
(use d :reload :verbose)

(def ow-config {:mp-domain-package-source 'ow.test.examples.my-domain :ontology-name "nescafe"})
)