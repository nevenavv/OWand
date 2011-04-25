(ns ow.my-domain
  (:use [org.uncomplicate.magicpotion]
        [org.uncomplicate.magicpotion.predicates]
        [ow.restrictions]))

; taken from magicpotion/examples/domain
(property pname
          [is-string?])

(property first-name
          [(min-length 3)]
          [pname])

(property last-name
          [(s-length-between 4 10)]
          [pname])

(property start-date
          [(before (java.util.Date.))])

(concept person
         [first-name
          last-name])

(concept professor
         [start-date]
         [person])

(property transcedental-property
          []
          [last-name])

(concept transcedental-being
         [transcedental-property]
         [professor])

(property knows
          [person?])

(property loves
          [person?])

;--- 
(property knows-professor
          :restrictions [professor?]
          :super [knows])
;---

(concept social-person
         [(ref> knows)
          (ref*> loves)]
         [person])

(concept social-person-by-val
         [(val> knows)
          (val*> loves)]
         [person])

(concept party
         [pname])

(property cname [is-string?])

(property company-name
         [(s-min-length 2)]
         [pname cname])
	
(concept company
         [(val> pname [(s-min-length 3)])
          (val> cname [#(= (first %) \A)])
          ;(val> company-name [(s-max-length 6)])
          company-name
          ]
         [party]
         [::company-name])

;------

;(property colorina)

(concept superman [cname])