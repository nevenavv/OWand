(ns ow.util
  (:require [clojure.contrib.str-utils2 :only [take replace] :as s ]
            [clojure.contrib.duck-streams :only [spit] :as ds])
  (:use [clojure.contrib.str-utils :only [re-sub]]
        [clojure.contrib.java-utils :only [file]]
        [clojure.contrib.core :only [seqable?]])
  (:import [java.util.regex Pattern]
           [java.net URLEncoder URLDecoder]))

;; formatters -------------
(defn datetime-for-xsd
  "Formatter for xsd:dateTime datatype (format: [-]CCYY-MM-DDThh:mm:ss[Z|(+|-)hh:mm]; ISO 8601)"
  [^java.util.Date d]
  (str (s/take (. (java.text.SimpleDateFormat. "yyyy-MM-dd'T'HH-mm-ssZ") format d) 22) ":00"))

;; string -----------------
(defn trim-leading-str
  ; from leiningen/jar.clj
  [s to-trim]
  (re-sub (re-pattern (str "^" (Pattern/quote to-trim))) "" s))

(defn trim-ending-str
  [s to-trim]
  (re-sub (re-pattern (str (Pattern/quote to-trim) "$" )) "" s))

(defn trim-ending
  [ending]
  (fn [trim-what] 
    (second (re-matches (re-pattern (str "(.*)" ending "$")) trim-what))))

(defn trim-ending-?
  "Returns trimed string if ends with '?', else nil."
  [thing]
  ((trim-ending "\\?") thing))

;; io ---------------------
(defn create-file
  [dir file-name source]
  (let [path (file (trim-leading-str dir "/"))
        _ (.mkdirs path)
        f (file path file-name)]
    (ds/spit f source)
    (println "Created" file-name "in:" (.getAbsolutePath path))))

;; ns ---------------------

(defn like
  ;; from clj-nstools
  "Take over all refers, aliases, and imports from the namespace named by
   ns-sym into the current namespace. Use :like in the ns+ macro in preference
   to calling this directly."
  [ns-sym]
  (require ns-sym)
  (doseq [[alias-sym other-ns] (ns-aliases ns-sym)]
    (alias alias-sym (ns-name other-ns)))
  (doseq [[sym ref] (ns-refers ns-sym)]
    (ns-unmap *ns* sym)
    (. *ns* (refer sym ref))
  (doseq [[sym ref] (ns-imports ns-sym)]
    (. *ns* (importClass ref)))))

;; seq --------------------

(defn assoc-new
   "Add kv pair if m doesn't have k and v is not nil."
   [m k v]
   (if (and ((comp not contains?) m k) v)
       (assoc m k v)
       m))

(defn flatten-1
  ;from clojuremvc
  "Flattens only the first level of a given sequence, e.g. [[1 2][3]] becomes
   [1 2 3], but [[1 [2]] [3]] becomes [1 [2] 3]."
  [sek]
  (if (or (not (seqable? sek)) (nil? sek))
    sek ; if seq is nil or not a sequence, don't do anything
    (loop [acc [] [elt & others] sek]
      (if (nil? elt) acc
        (recur
          (if (seqable? elt)
            (apply conj acc elt) ; if elt is a sequence, add each element of elt
            (conj acc elt))      ; if elt is not a sequence, add elt itself directly
          others)))))

(defn flatten-1-
  ; from clj-plaza, not same res
  ([seq]
     (let [old-meta (meta seq)
           ts-pre (reduce (fn [acum item]
                            (if (and (coll? item) (coll? (first item)))
                              (concat acum item)
                              (conj acum item)))
                          []
                          seq)
           ts (vec (distinct ts-pre))]
       (with-meta ts old-meta))))

;; xml --------------------
(defn encode
  ; from ring (url-encode)
  [unencoded & [encoding]]
  (URLEncoder/encode unencoded (or encoding "UTF-8")))

(defn encoded-datetime-for-xsd
  [d]
  (encode (datetime-for-xsd d)))

(defn decode
  ;from ring (url-decode)
  [encoded & [encoding]]
  (URLDecoder/decode encoded (or encoding "UTF-8")))

;; ow ---------------------
(defn ont-name-for-pr [ont-config]
  ; and slashes in name (iri..)??
  (s/replace (:ontology-name ont-config) #" " "-"))

(defn ont-full-ns [ont-config]
  (str (trim-ending-str (:ont-root-domain-ns ont-config) "/") "/" (ont-name-for-pr ont-config)))
