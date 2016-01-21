(ns graftwerk.js-test
  (:require [graftwerk.js :refer :all]
            [clojure.test :refer :all]
            [clj-rhino :as js]))

(deftest js-fn-no-timeout-test
  (testing "Basic javascript function call"
    (let [add (js-fn-no-timeout "function(a, b) { return a + b; }")]
      (is (= 30.0 (add 10 20))))

    (let [maplookup (js-fn-no-timeout "function(a) { return a['bar']; }")]
      (is (= "baz" (maplookup {:bar "baz"}))))

    (testing "supports Date objects"
      (let [date (js-fn-no-timeout "function(a) { return a; }")]
        (is (instance? java.util.Date (date (java.util.Date.)))))

      (let [date (js-fn-no-timeout "function() { return new Date(); }")]
        (is (instance? java.util.Date (date))))))

  (testing "Invalid javascript raises errors at compile time"
    (is (thrown? org.mozilla.javascript.EvaluatorException
                 (js-fn-no-timeout "function(a, b) { ;; deliberate syntax error ")))))
