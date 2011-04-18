(ns ow.engine
 (:use [clojure.contrib.java-utils :only [file]]
       [clojure.set :only [select union]]
       [ow.util :only [trim-ending-?]])
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
(def concepts) ; set of maps- {:name concept-symbol :roles [roles-expr] :super [super-symbols] :restrictions [restrictions-expr]}
(def dt-properties) ; set of maps- {:name dt-property-symbol :all-restrictions [restrictions-expr]}
(def obj-properties) ;set of maps- {:name obj-property-symbol :super [super-symbols] :ranges [obj-prop-range-symbol]}

(defn assort-things
  "Assort things from MP model and bound to vars."
  [mp-model]
  (let [assorted  (reduce (fn [res el] 
								             (let [thing (first el)
								                   thing-name (second el)
                                   params (drop 2 el)]
								                (cond (= 'concept thing)
							                           (let [pos (take-while (comp not keyword?) params)
                                               kw-map (apply hash-map (drop-while (comp not keyword?) params))
                                               roles (if-let [r (first pos)] r (:roles kw-map))
																	             super (if-let [s (second pos)] s (:super kw-map))
                                               restrictions (set (if-let [re (second (rest pos))] re (:restrictions kw-map)))]
								                            (merge-with conj res {:concepts {:name thing-name :roles roles :super super :restrictions restrictions}
                                                                  :concept-names thing-name})) 
								                      (= 'property thing) 
								                         (let [pos (take-while (comp not keyword?) params)
                                               kw-map (apply hash-map (drop-while (comp not keyword?) params))
                                               restrictions (if-let [r (first pos)] r (:restrictions kw-map))
								                               super (if-let [s (second pos)] s (:super kw-map))
								                               obj-prop-ranges (filter identity (map #(if-let [predicate-name (trim-ending-? (str %))] 
								                                                                        (symbol predicate-name)) 
								                                                                  restrictions))]
								                           (if (or ;some restriction is '<consept-name>?'
								                                 (some (:concept-names res) obj-prop-ranges)
								                                 ;some super is in obj-properties
								                                 (some (:obj-property-names res) super))
								                            (merge-with conj res {:obj-properties {:name thing-name :super super :ranges obj-prop-ranges}
                                                                  :obj-property-names thing-name})
								                            (let [all-restrictions (reduce (fn [r papa] (union r (:all-restrictions (first (select #(= papa (:name %)) (:dt-properties res)))))) 
                                                                      (set restrictions) super)]
                                               (merge-with conj res {:dt-properties {:name thing-name :all-restrictions all-restrictions}}))))
								                       :else res)))
								           {:concepts #{} 
								            :dt-properties #{}
								            :obj-properties #{}
                            ;helpers
                            :concept-names #{}
                            :obj-property-names #{}}
								          (gather-things mp-model))]
    (intern 'ow.engine 'concepts (:concepts assorted))
    (intern 'ow.engine 'dt-properties (:dt-properties assorted))
    (intern 'ow.engine 'obj-properties (:obj-properties assorted))))
