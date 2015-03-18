(ns graftwerk.validations-test
  (:require [graftwerk.validations :refer :all] :reload)
  (:require
   [clojure.test :refer :all]
   [bouncer.core :as b]
   [bouncer.validators :as v]))

(def valid-params {:page 1
                   :pipeline {:content-type "application/clojure"}
                   :data {:content-type "application/csv"}})

(deftest validate-evaluation-request-test
  (testing "Valid parameters"
    (is (b/valid?
         (validate-evaluation-request valid-params))))
  (testing "Invalid parameters"
    (is (validate-evaluation-request (assoc valid-params :page "foo")))

    (is (-> valid-params
            (assoc-in [:pipeline :content-type] "foo/bar")
            validate-evaluation-request
            valid?
            not))

    (is (-> valid-params
            (assoc-in [:data :content-type] "foo/bar")
            validate-evaluation-request
            valid?
            not))))
