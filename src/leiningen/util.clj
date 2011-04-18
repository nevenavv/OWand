(ns leiningen.util)

(defn assoc-new
   "Add kv pair if m doesn't have k and v is not nil."
   [m k v]
   (if (and ((comp not contains?) m k) v)
       (assoc m k v)
       m))

(defn get-config [project]
  (let [config (:ow-config project)
        url (:url project)
        nm (:name project)]
    (-> config
      (assoc-new :ont-root-domain-ns (str (trim-ending-str url "/") "/ontologies/"))
      (assoc-new :ontology-name nm))))
