(ns ow.test.restrictions-test
  (:use [clojure.test])
  (:use [ow.restrictions :as r]))

(deftest test-of-type
  (is (= :string (of-type "Bryan Adams")))
  (is (= :integer (of-type 69)))
  (is (= :double (of-type 0.09)))
  (is (= :dateTime (of-type (java.util.Date.)))))

(deftest test-not-nil?
  (is (= true (not-nil? :cranberries)))
  (is (= false (not-nil? nil)))
  (is (= ::r/not-nil (:restriction (meta not-nil?)))))

(deftest test-card
  (is (= true ((card 2) #{1 2})))
  (is (= false ((card 2) #{1 2 3})))
  (is (= :eq (:restriction (meta (card 2))))))

(deftest test-min-card
  (is (= true ((min-card 2) #{1 2})))
  (is (= true ((min-card 2) #{1 2 3})))
  (is (= false ((min-card 2) #{1})))
  (is (= false ((min-card 2) #{})))
  (is (= :min (:restriction (meta (min-card 2))))))

(deftest test-max-card
  (is (= true ((max-card 2) #{1 2})))
  (is (= true ((max-card 2) #{2})))
  (is (= true ((max-card 2) #{})))
  (is (= false ((max-card 2) #{1 2 3})))
  (is (= :max (:restriction (meta (max-card 2))))))

;; TODO
(deftest test-s-length-between
  (is (= true ((s-length-between 4 8) "toshiba")))
  (is (= false ((s-length-between 4 8) "msi"))))
