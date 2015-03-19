(ns graftwerk.routes.evaluate
  (:require [compojure.core :refer [defroutes POST]]
            [clojure.java.io :as io]
            [grafter.tabular :refer [make-dataset dataset?]]
            [clojail.core :refer [sandbox safe-read]]
            [clojail.jvm :refer [permissions domain context]]
            [taoensso.timbre :as log]
            [clojail.testers :refer [secure-tester-without-def]]
            [graftwerk.validations :refer [if-invalid valid? validate-pipe-run-request validate-graft-run-request]]
            [grafter.pipeline :as pl])
  (:import [java.io FilePermission]))

(def default-namespace 'graftwerk.pipeline)

(defn namespace-declaration []
  (let [requires '(:require
                   [grafter.tabular :refer [defpipe defgraft column-names columns rows
                                            all-columns derive-column mapc swap drop-rows
                                            read-dataset read-datasets make-dataset
                                            move-first-row-to-header _ graph-fn
                                            test-dataset]]
                   [grafter.rdf :refer [s prefixer]]
                   [grafter.rdf.protocols :refer [->Quad]]
                   [grafter.rdf.templater :refer [graph]]
                   [grafter.vocabularies.rdf :refer :all]
                   [grafter.vocabularies.foaf :refer :all])]
    `(ns ~default-namespace
       ~requires)))

(defn namespace-qualify [command]
  (symbol (str default-namespace "/" command)))

(defn build-sandbox [pipeline-sexp data]
  (let [context (-> (FilePermission. data "read")
                    permissions
                    domain
                    context)

        sb (sandbox secure-tester-without-def
                    :init (namespace-declaration)
                    :namespace  default-namespace
                    :context context
                    :max-defs 500)]
    (sb pipeline-sexp)
    sb))

(defn evaluate-command [sandbox command data]
  (let [apply-pipe (list command data)]
    (log/info "About to apply pipe in sandbox" apply-pipe)
    (sandbox apply-pipe)))

(def default-page-size "50")

(defn paginate-seq [results page-size page-number]
  (if (and page-number (not (empty? page-number)))
    (let [page-number (Integer/parseInt page-number)
          page-size (Integer/parseInt (or page-size default-page-size))]
      (log/info "Paging results " page-size " per page.  Page #" page-number)
      (->>  results
            (drop (* page-number page-size))
            (take page-size)))
    results))

(defn paginate
  "Paginate the supplied dataset."
  [ds page-size page-number]

  (make-dataset (paginate-seq (:rows ds)
                              page-size page-number)
                (:column-names ds)))

(defn read-pipeline [pipeline]
  (-> pipeline :tempfile slurp safe-read))

(defn execute-pipe
  "Takes the data to operate on (a ring file map) a command (a
  function name for a pipe or graft) and a pipeline clojure file and
  returns a Grafter dataset."
  [data command pipeline]
  (let [command (symbol command)
        transformation (read-pipeline pipeline)
        data-file (-> data :tempfile .getPath)
        sandbox (build-sandbox transformation data-file)]
    (evaluate-command sandbox command data-file)))

(defroutes pipe-route
  (POST "/evaluate/pipe" {{:keys [pipeline data page-size page command] :as params} :params}
        (if-invalid [errors (validate-pipe-run-request params)]
                     {:status 422 :body errors}
                     {:status 200 :body (-> data
                                           (execute-pipe command pipeline)
                                           (paginate page-size page))})))

(defn execute-graft [data command pipeline]
  (let [forms (read-pipeline pipeline)
        command (symbol command)
        data-file (-> data :tempfile .getPath)
        sandbox (build-sandbox forms data-file)]

    (evaluate-command sandbox command data-file)))

;; TODO support this route without pagination
(defroutes graft-route
  (POST "/evaluate/graft" {{:keys [pipeline data command] :as params} :params}
        (if-invalid [errors (validate-graft-run-request params)]
                    {:status 422 :body errors}
                    {:status 200 :body (execute-graft data command pipeline)})))

;;
;; Items below this line are largely unimplemented...
;;

(defn find-graft [name forms]
  (if-let [graft (first (filter (fn [g]
                             (when (and (= :graft (:type g))
                                        (= :name (:name g)))
                               g))
                                (pl/find-pipelines forms default-namespace {})))]
    graft
    (throw (RuntimeException. "Could not find graft" name))))


(comment ;; TODO
  (defn execute-graft-with-row [row data command pipeline]
    (let [forms (read-pipeline pipeline)
          command (symbol command)
          pipe-command (-> forms
                           (pl/find-pipelines default-namespace {})
                           (find-graft command)
                           :body
                           last)
          ds (-> data
                 (execute-pipe pipe-command pipeline))]
      ;; TODO
      )))
