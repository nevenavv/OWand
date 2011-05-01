(ns ow.engine
  (:use [clojure.contrib.java-utils :only [file]]
        [clojure.set :only [select union intersection]]
        [ow.util :only [trim-ending-?]]
        [ow.engine-util])
  (:require [clojure.contrib.str-utils2 :only [replace] :as s])
  (:import [java.io File FileReader BufferedReader PushbackReader]))
	
(defn- gather-things 
  "Returs list of statements read from clj file of given ns." ;root dir is 'src/', so for ns one.two.three
  [domain-ns]                                                 ;it's looking for 'src/one/two/three.clj'... [not great]
  (let [f (file (str "src/" (-> (name domain-ns)
                              (s/replace "." "/")
                              (s/replace "-" "_")) ".clj"))]
    (with-open [rdr (PushbackReader. (BufferedReader. (FileReader. f)))]
      (loop [res []
             a (read rdr false :eof)]
        (if (= a :eof)
          res
          (recur (conj res a) (read rdr false :eof)))))))
	
; bounded in 'assort-things'
(def concepts) ; set of maps- {:name concept-symbol :roles [roles-expr] :super [super-symbols] :restrictions [restrictions-expr] :props-domain props-domain :disjoints #{disjoint-with-concepts} :roles-contruct [roles-construct]}
(def dt-properties) ; set of maps- {:name dt-property-symbol :restrictions #{restrictions} :super [super]}
(def obj-properties) ;set of maps- {:name obj-property-symbol :super [super-symbols] :ranges [obj-prop-range-symbol]}
(def obj-properties-ranges)
(def properties-domain) ; map of pais {property-symbol [domain-concepts]}
	
(defn assort-things
  "Assort things from MP model and bound to vars."
  [mp-model]
  (let [assorted (reduce (fn [res el] 
                           (let [thing (first el)
                                 thing-name (second el)
                                 params (drop 2 el)]
                             (cond 
                               (= 'concept thing)
                               (let [pos (take-while (comp not keyword?) params)
                                     kw-map (apply hash-map (drop-while (comp not keyword?) params))
                                     roles (if-let [r (first pos)] r (:roles kw-map))
                                     super (if-let [s (second pos)] s (:super kw-map))
                                     restrictions (set (if-let [re (second (rest pos))] re (:restrictions kw-map)))
                                     properties- (map #(get-prop-name %) roles)
                                     concept-rests-nn-props (intersection (set (map #(if-not (many-role? %) (get-prop-name %)) properties-)) 
                                                                          (set (filter identity (map #(if (keyword? %) (symbol (name %))) restrictions))))
                                     props-domain (zipmap properties- (repeat [thing-name]))
                                     roles-construct (filter identity (map #(let [property-name (get-prop-name %)
                                                                                  property-type (if (some #{property-name} (:obj-property-names res)) :object :datatype)
                                                                                  role-restrictions (get-role-restrictions %)
                                                                                  not-nil- (or (is-not-nil-property? role-restrictions) 
                                                                                               (some #{property-name} (concat concept-rests-nn-props (:not-nil-properties res))))
                                                                                  role-set-restrictions (get-role-set-restrictions %)
                                                                                  cardinality (if (many-role? %)
                                                                                                (get-cardinality-map role-set-restrictions)
                                                                                                (hash-map (if not-nil- :eq :max) 1))]
                                                                              (if-not (every? empty? [cardinality role-restrictions])
                                                                                {:property-name property-name :property-type property-type :restrictions role-restrictions :cardinality cardinality}))
                                                                        roles))]
                                 (merge-with conj res {:concepts {:name thing-name :roles roles :super super :restrictions restrictions :props-domain props-domain :roles-construct roles-construct}
                                                       :concept-names thing-name})) 
                               (= 'property thing) 
                               (let [pos (take-while (comp not keyword?) params)
                                     kw-map (apply hash-map (drop-while (comp not keyword?) params))
                                     restrictions (if-let [r (first pos)] r (:restrictions kw-map))
                                     super (if-let [s (second pos)] s (:super kw-map))
                                     range-concepts (filter identity (map #(if-let [predicate-name (trim-ending-? (str %))] 
                                                                             (if (contains? (:concept-names res) (symbol predicate-name))
                                                                               (symbol predicate-name))) 
	                                                                         restrictions))
                                     nn-properties (if (is-not-nil-property? restrictions) thing-name)]
                                 (if (or (seq range-concepts) (some (:obj-property-names res) super))
                                   (merge-with conj res {:obj-properties {:name thing-name :super super :ranges range-concepts}
                                                         :obj-property-names (with-meta thing-name {:ranges range-concepts})
                                                         :not-nil-properties nn-properties})
                                   (merge-with conj res {:dt-properties {:name thing-name  :super super :restrictions (set restrictions)}
                                                         :not-nil-properties nn-properties})))
                               :else res)))
                   {:concepts #{} 
                    :dt-properties #{}
                    :obj-properties #{}
                    ;helpers
                    :concept-names #{}
                    :obj-property-names #{}
                    :not-nil-properties #{}}
                   (gather-things mp-model))
        supers-childern- (reduce #(merge-with concat %1 
                                    (zipmap (if (seq (:super %2)) (:super %2) [nil]) (repeat [(:name %2)]))) 
                           {} (:concepts assorted))
        concepts-with-disjoints (map (fn [c] (assoc c :disjoints 
                                               (disj (set (apply concat (filter #(some #{(:name c)} %) (vals supers-childern-)))) (:name c)))) 
                                  (:concepts assorted))]
    (intern 'ow.engine 'concepts concepts-with-disjoints)
    (intern 'ow.engine 'dt-properties (:dt-properties assorted))
    (intern 'ow.engine 'obj-properties (:obj-properties assorted))
    (intern 'ow.engine 'obj-properties-ranges (:obj-property-names assorted))
    (intern 'ow.engine 'properties-domain (reduce #(merge-with concat %1 (:props-domain %2)) {} (:concepts assorted)))))
