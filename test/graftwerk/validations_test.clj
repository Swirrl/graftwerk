(ns graftwerk.validations-test
  (:require [graftwerk.validations :refer :all] :reload)
  (:require
   [clojure.test :refer :all]
   [bouncer.core :as b]
   [bouncer.validators :as v]))

(def valid-params {:page 1
                   :pipeline {:filename "foo.clj"}
                   :data {:filename "bar.csv"}
                   :command "my-foo-pipe-or-graft"})

(defn dissoc-in
  "Yes this is really missing from clojure.core :-()"
  [m path]
  (update-in m (drop-last path) (fn [v] (dissoc v (last path)))))

(deftest validate-pipe-run-request-test
  (testing "Valid parameters"
    (is (b/valid?
         (validate-pipe-run-request valid-params))))
  (testing "Invalid parameters"
    (is (validate-pipe-run-request (assoc valid-params :page "foo"))
        "Must be an integer or nil")

    (is (not (-> valid-params
                 (dissoc-in [:pipeline :filename])
                 validate-pipe-run-request
                 valid?))
        "Must have filename")))

(deftest validate-graft-run-request-test
  (testing "Valid parameters"
    (is (b/valid?
         (validate-graft-run-request valid-params))))
  (testing "Invalid parameters"
    (is (validate-graft-run-request (assoc valid-params :page "foo"))
        "Must be an integer or nil")

    (is (not (-> valid-params
                 (dissoc-in [:pipeline :filename])
                 validate-graft-run-request
                 valid?))
        "Must have a filename")))
