(ns ow.test.util-test
  (:use [clojure.test])
  (:use [ow.util]))

;; formatters -------------
(deftest test-datetime-for-xsd
  (is (= "2012-12-22T00-00-00+01:00" (datetime-for-xsd (.parse (java.text.SimpleDateFormat. "dd/MM/yyyy") "22/12/2012")))))

;; string -----------------
(deftest test-trim-leading-str
  (is (= "folder" (trim-leading-str "/folder" "/")))
  (is (= "folder" (trim-leading-str "folder" "/"))))

(deftest test-trim-ending-str
  (is (= "/folder" (trim-ending-str "/folder/" "/")))
  (is (= "folder" (trim-ending-str "folder" "/"))))

;; seq --------------------
(deftest test-flatten-1
  (is (= [1 2 3] (flatten-1 [1 [2] 3])))
  (is (= [1 [2] 3] (flatten-1 [1 [[2]] 3])))
  (is (= [1 2 [3] [4 5 [6]] 7] (flatten-1 [1 2 [[3] [4 5 [6]]] 7]))))

(deftest test-assoc-new
  (is (= {:a 1 :b 2 :c 3} (assoc-new {:a 1 :b 2 :c 3} :a 5)))
  (is (= {:a 1 :b 2 :c 3} (assoc-new {:a 1 :b 2 :c 3} :d nil)))
  (is (= {:a 1 :b 2 :c 3 :d 4} (assoc-new {:a 1 :b 2 :c 3} :d 4))))

;; ow ---------------------
(deftest test-ont-name
  (is (= "my-ontology" (ont-name {:ontology-name "my ontology"}))))

(deftest test-ont-full-ns
  (is (= "http://www.meme.org/my-ontology" (ont-full-ns {:ont-root-domain-ns "http://www.meme.org" :ontology-name "my ontology"})))
  (is (= "http://www.meme.org/my-ontology" (ont-full-ns {:ont-root-domain-ns "http://www.meme.org/" :ontology-name "my ontology"}))))