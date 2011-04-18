(ns ow.restrictions
  (:use [robert.hooke]
        [org.uncomplicate.magicpotion.predicates]))

(defn of-type
  [x]
  (cond (string? x) :string 
 			  (instance? java.lang.Integer x) :integer
	   	  (instance? java.lang.Double x) :double
			  (instance? java.util.Date x) :dateTime))

;; misc --------------------------------
(def not-nil?
  (with-meta
    (fn [prop]
      (if (= nil prop) false true))
  {:restriction ::not-nil}))

(defn has-value
  [v]
  (with-meta
    (fn [x]
       (= v x))
    {:restriction ::has-value
     :restriction-with v
     :restriction-on (of-type v)}))

(defn in
  [coll]
  {:pre [(seq coll)]}
  (with-meta
    (fn [x]
       (some #(= x %) coll))
    {:restriction ::in
     :restriction-with coll
     :restriction-on (of-type (first coll))}))

;; type restrictions ----------------------
(def is-string?
  (with-meta
    string?
    {:restriction ::type
	   :restriction-on :string}))

(def is-int?
  (with-meta
    (fn [x]
      (instance? java.lang.Integer x))
    {:restriction ::type
	   :restriction-on :integer}))

(def is-double?
  (with-meta
    (fn [x]
      (instance? java.lang.Double x))
    {:restriction ::type
	   :restriction-on :double}))

(def is-date?
  (with-meta
    (fn [x]
      (instance? java.util.Date x))
    {:restriction ::type
     :restriction-on :dateTime}))

;; string restrictions ------------------

(defn s-min-length
  [low]
  (with-meta
	  (fn [#^String s]
	    (<= low (.length s)))
	  {:restriction ::min-length
	   :restriction-with low
     :restriction-on :string}))

(defn s-max-length
  [high]
  (with-meta
	  (fn [#^String s]
	    (>= high (.length s)))
	  {:restriction ::max-length
	   :restriction-with high
     :restriction-on :string}))

(defn s-length
  [v]
  (with-meta
	  (fn [#^String s]
	    (= v (.length s)))
    {:restriction ::length
     :restriction-with v
     :restriction-on :string}))

(defn s-length-between
  [l h]
  (with-meta
	  (fn [#^String s]
	     (and  (>= (.length s) l) (<= (.length s) h)))
    {:restriction ::s-between
     :restriction-with [l h]
     :restriction-on :string}))

(defn s-pattern
  [re]
  (with-meta
	  (fn [#^String s]
	    (= s (re-find re s)))
	    {:restriction ::pattern
	     :restriction-with re ;java pattern != xsd pattern syntax!
       :restriction-on :string}))

;; number restrictions -----------------
(defn lt
  [n]
  (with-meta
	  (fn [x]
	    (< x n))
	    {:restriction ::lt
	     :restriction-with n
       :restriction-on (of-type n)}))

(defn gt
  [n]
  (with-meta
	  (fn [x]
	    (> x n))
	    {:restriction ::gt
	     :restriction-with n
       :restriction-on (of-type n)}))

(defn n-between
  [l g]
  (with-meta
	  (fn [x]
	    (and (> x l) (< x g)))
	    {:restriction ::n-between
	     :restriction-with [l g]
       :restriction-on (of-type l)}))

;; dateTime restrictions --------------------
(defn before
  [t]
  (with-meta
	  (fn [#^java.util.Date d]
	    (. d before t))
	    {:restriction ::before
	     :restriction-with t
       :restriction-on :dateTime}))

(defn after
  [t]
  (with-meta
	  (fn [#^java.util.Date d]
	    (. d after t))
	    {:restriction ::after
	     :restriction-with t
       :restriction-on :dateTime}))

(defn d-between
  [b a]
  (with-meta
	  (fn [#^java.util.Date d]
	    (and (. d before b) (. d after a)))
	    {:restriction ::d-between
	     :restriction-with [b a]
       :restriction-on :dateTime}))

;; prepare MP predicates - restriciton fns
(defn switch-min-length
  [f low]
  (s-min-length low))

(defn switch-max-length
  [f high]
  (s-max-length high))

(defn switch-length-between
  [f low high]
  (s-length-between low high))

(defn switch-between
  [f low high]
  (n-between low high))

(defn switch-string?
  [f x]
  (is-string? x))
  
(add-hook #'min-length switch-min-length)
(add-hook #'max-length switch-max-length)
(add-hook #'length-between switch-length-between)
(add-hook #'between switch-between)
(add-hook #'string? switch-string?)
