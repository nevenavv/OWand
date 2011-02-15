(ns ow.test.IRI-test
  (:use [clojure.test])
  (:use [ow.IRI]))

(comment
(deftest test-iri-expand
  (is (= "http://www.w3.org/2002/07/owl#something" (iri-expand :owl:something)))
  (is (thrown? Exception (iri-expand :undefined:something))))
)

