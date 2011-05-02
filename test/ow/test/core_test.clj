(ns ow.test.core-test
  (:use [ow.core])
  (:use [clojure.test]))

;; try examples/start
(comment
  (ow-export-goal)
  
  (.printStackTrace *e)
)

(comment 
 
  (use 'clojure.contrib.pprint)
  
  
  
  (pprint (loaded-libs))
  
  
  (pprint (all-ns))
  
  (pprint (ns-refers (find-ns 'ow.my-domain)))
  
  (pprint (load-string (ns ow.my-domain (:use [org.uncomplicate.magicpotion] [org.uncomplicate.magicpotion.predicates] [ow.restrictions])))) 
 
)