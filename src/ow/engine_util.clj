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