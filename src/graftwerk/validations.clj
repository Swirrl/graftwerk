(ns graftwerk.validations
  (:require [bouncer.core :as b]
            [bouncer.validators :as v]))

(defn try-convert [f]
  (fn [v]
    (try
      (f v)
      (catch Exception ex
        v))))

(def try->integer (try-convert #(Integer/parseInt %)))

(v/defvalidator empty-or-number {:optional true} [i]
  (cond
    (nil? i) true
    (number? i) true
    (= "" i) true
    :else false))

(v/defvalidator clojure-content
  {:default-message-format "Content type of file is not application/clojure"}
  [mime-type]
  (= "application/clojure" mime-type))

(v/defvalidator tabular-file
  {:default-message-format "Content type of file is not application/csv"}
  [mime-type]
  (#{"application/csv"} mime-type))


(defn validate-evaluation-request [params]
  (b/validate params
              :page empty-or-number
              :page-size empty-or-number
              [:pipeline :content-type] clojure-content
              [:data :content-type] tabular-file))

;; This seems more logical than the implementations in bouncer to
;; me. Should investigate this when we have more time and perhaps
;; suggest improvements.

(defn valid? [validation-result]
  (let [[errors data] validation-result]
    (nil? errors)))

(defmacro if-invalid
  "Like an if-let but with validation results."
  [[bound-var test-form] invalid-body valid-body]

  `(let [validation-result# ~test-form]
     (if-let [~bound-var (when (not (valid? validation-result#))
                           (first validation-result#))]
       ~invalid-body
       ~valid-body)))
