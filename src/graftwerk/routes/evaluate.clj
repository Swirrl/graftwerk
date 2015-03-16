(ns graftwerk.routes.evaluate
  (:require [compojure.core :refer [defroutes POST]]
            [clojure.java.io :as io]
            [grafter.tabular :refer [make-dataset]]
            [clojail.core :refer [sandbox safe-read]]
            [clojail.jvm :refer [permissions domain context]]
            [taoensso.timbre :as log]
            [clojail.testers :refer [secure-tester-without-def]])
  (:import [java.io FilePermission]))

(def default-page-size 50)

(def default-namespace 'graftwerk.pipeline)

(def transformation-namespace '(ns graftwerk.pipeline
                                 (:require
                                  [grafter.tabular :refer [defpipe defgraft column-names columns rows
                                                           all-columns derive-column mapc swap drop-rows
                                                           read-dataset read-datasets make-dataset
                                                           move-first-row-to-header _ graph-fn
                                                           test-dataset]]
                                  [grafter.rdf :refer [s prefixer]]
                                  [grafter.rdf.protocols :refer [->Quad]]
                                  [grafter.rdf.templater :refer [graph]]
                                  [grafter.vocabularies.rdf :refer :all]
                                  [grafter.vocabularies.foaf :refer :all])))

(defn namespace-qualify [command]
  (symbol (str default-namespace "/" command)))

(defn evaluate-command [pipeline command data]
  (let [sb (sandbox secure-tester-without-def
                    :init transformation-namespace
                    :namespace default-namespace
                    :context (-> (FilePermission. data "read")
                                 permissions
                                 domain
                                 context))
        execute-pipe `(~command ~data)]
    (sb pipeline)
    (sb execute-pipe)))


(defn paginate-seq [results page-size page-number]
  (if page-number
    (->>  results
          (drop (* page-number (or page-size default-page-size)))
          (take page-size))
    results))

(defn paginate
  "Paginate the supplied dataset."

  [ds page-size page-number]

  (make-dataset (paginate-seq (:rows ds) page-size page-number)
                (:column-names ds)))

(defroutes pipe-route
  (POST "/evaluate/pipe" {{:keys [pipeline data page-size page command]} :params}
        (try
          (let [command (namespace-qualify command)
                transformation (-> pipeline :tempfile slurp safe-read)
                data-file (-> data :tempfile .getPath)
                result-ds (evaluate-command transformation command data-file)
                paged-results (paginate result-ds page-size page)]


            {:status 200 :body paged-results})
          (catch Exception ex
            (log/warn ex)
            {:status 500 :body {:type :error
                                :message (.getMessage ex)
                                :class (str (.getClass ex))}}))))

(defroutes graft-route
  (POST "/evaluate/graft" []
        {:status 200 :body {:foo :bar :baz :graft}}))
