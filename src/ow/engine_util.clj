(ns ow.engine-util)

(defn is-not-nil-property?
  [restrictions]
  (some #(or (= 'not-nil? %) (= '(comp not nil?) %)) restrictions))
	
(defn get-prop-name
  [role]
  (cond 
    (symbol? role) role 
    (coll? role) (second role)))

(defn many-role?
  [role]
  (and (coll? role) (.endsWith (str (first role)) "*>")))

(defn get-role-restrictions
  [role]
  (if (coll? role) (second (rest role))))

(defn get-role-set-restrictions
  [role]
  (if (coll? role) (second (rest (rest role)))))

(defn get-cardinality-map
  [restrictions]
  (reduce #(let [{r :restriction v :restriction-with on :restriction-on} (meta (eval %2))]
             (if (= :cardinality on)
               (assoc %1 r v)
               %1))
    {} restrictions))
  