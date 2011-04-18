(ns ow.test.engine-test)

(def b 'ow.my-domain)



(def ff (let [f (file (str "src/" (-> (name b)
                              (s/replace "." "/")
                              (s/replace "-" "_")) ".clj"))]
  f))

(gather-things 'ow.my-domain)

(reset! *things* [])

@*things*

(def s (eval (second @*things*)))

(use '[org.uncomplicate.magicpotion]
     '[org.uncomplicate.magicpotion.predicates]
     '[ow.restrictions])

(assort-things 'ow.my-domain)
(time (assort-things 'ow.my-domain)) 
