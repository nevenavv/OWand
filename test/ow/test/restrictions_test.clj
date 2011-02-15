(ns ow.test.restrictions-test
  (:use [clojure.test])
  (:use [ow.restrictions]))

(deftest test-of-type
  (is (= :string (of-type "Bryan Adams")))
  (is (= :integer (of-type 69)))
  (is (= :double (of-type 0.09)))
  (is (= :dateTime (of-type (java.util.Date.)))))

(deftest test-not-nil?
  (is (= true (not-nil? :cranberries)))
  (is (= false (not-nil? nil)))
  (is (= :ow.restrictions/not-nil (:restriction (meta not-nil?)))))

;; TODO
(deftest test-s-length-between
  (is (= true ((s-length-between 4 8) "toshiba")))
  (is (= false ((s-length-between 4 8) "msi"))))
