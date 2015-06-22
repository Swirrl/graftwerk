(ns graftwerk.routes.evaluate
  (:require [compojure.core :refer [defroutes POST]]
            [clojure.stacktrace :refer [root-cause]]
            [clojure.java.io :as io]
            [clojure.string :refer [trim]]
            [grafter.tabular :refer [make-dataset dataset?]]
            [clojail.core :refer [sandbox safe-read eagerly-consume]]
            [clojail.jvm :refer [permissions domain context]]
            [taoensso.timbre :as log]
            [clojail.testers :refer [secure-tester-without-def blanket blacklist-objects blacklist-packages blacklist-symbols blacklist-nses]]
            [graftwerk.validations :refer [if-invalid valid? validate-pipe-run-request validate-graft-run-request]]
            [grafter.pipeline :as pl]
            [taoensso.timbre :as log])
  (:import [java.io FilePermission]
           (clojure.lang LispReader$ReaderException)))

(def default-namespace 'graftwerk.pipeline)

;; TODO load this declaration from a file editable by the devops team.
(defn namespace-declaration []
  (let [requires '(:require
                   [grafter.tabular :refer [defpipe defgraft column-names columns rows
                                            derive-column mapc swap drop-rows
                                            read-dataset read-datasets make-dataset
                                            move-first-row-to-header _ graph-fn
                                            test-dataset]]
                   [clojure.java.io]
                   [grafter.rdf.preview :refer [preview-graph]]
                   [grafter.rdf :refer [s prefixer]]
                   [grafter.rdf.protocols :refer [->Quad]]
                   [grafter.rdf.templater :refer [graph]]
                   [grafter.vocabularies.rdf :refer :all]
                   [grafter.vocabularies.foaf :refer :all])]
    `(ns ~default-namespace
       ~requires)))

(defn namespace-qualify [command]
  (symbol (str default-namespace "/" command)))

(def ^{:doc "A tester that attempts to be secure, and allows def."}
  modifed-secure-tester-without-def
  [(blacklist-objects [clojure.lang.Compiler clojure.lang.Ref clojure.lang.Reflector
                       clojure.lang.Namespace clojure.lang.Var clojure.lang.RT
                       java.io.ObjectInputStream])
   (blacklist-packages ["java.lang.reflect"
                        "java.security"
                        "java.util.concurrent"
                        "java.awt"])
   (blacklist-symbols
    '#{alter-var-root intern eval catch *read-eval*
       load-string load-reader addMethod ns-resolve resolve find-var
       ns-publics ns-unmap set! ns-map ns-interns the-ns
       push-thread-bindings
       pop-thread-bindings
       future-call agent send
       send-off pmap pcalls pvals in-ns System/out System/in System/err
       with-redefs-fn Class/forName})
   (blacklist-nses '[clojure.main])
   (blanket "clojail")])

(defn build-sandbox
  "Build a clojailed sandbox configured for Grafter pipelines.  Takes
  a parsed sexp containing the grafter pipeline file."
  [pipeline-sexp file-path]
  (let [context (-> (FilePermission. file-path "read")
                    permissions
                    domain
                    context)

        sb (sandbox secure-tester-without-def
                    :init (namespace-declaration)
                    :namespace  default-namespace
                    :context context
                    :transform eagerly-consume
                    :max-defs 500)]
    (log/log-env :info "build-sandbox")
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

(defn read-pipeline
  "Takes a ring style multi-part form map that contains a file reference to
  a :tempfile, reads the s-expressions out of the file and returns it wrapped in
  a '(do ...) for evaluation."
  [pipeline]

  (let [code (-> pipeline :tempfile slurp)]

    ;; FUGLY hack beware!!!
    ;;
    ;; read/read-string and safe-read only read one
    ;; form, not all of them from a string.  So we need to wrap the
    ;; forms up into one.
    ;;
    ;; TODO clean this up!
    (safe-read (str "(do "
                    code
                    ")"))))

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

;;
;; Items below this line are largely unimplemented...
;;

(defn find-graft [pipelines-seq name]
  (if-let [graft (first (filter (fn [g]
                                  (and (= :graft (:type g))
                                       (= name (:name g))))
                                pipelines-seq))]
    graft
    (throw (RuntimeException. (str "Could not find graft " name)))))

(defn find-pipe-for-graft [pipeline-forms graft-command]
  (let [graft-command (symbol graft-command)
        graft-comp (-> pipeline-forms
                       (pl/find-pipelines default-namespace {})
                       (find-graft graft-command)
                       :body)
        pipe-sym (last graft-comp) ;; find pipe-command its the last '(comp .. .. pipe-command)]
        template-sym (second graft-comp)]
    [pipe-sym template-sym]))

(defn preview-graft-with-row [row
                              {data-file :tempfile :as data}
                              graft-command
                              {:keys [tempfile] :as pipeline}]
  (let [graft-sym (symbol graft-command)
        pipeline-forms (read-pipeline pipeline)
        [pipe-sym template-sym] (find-pipe-for-graft pipeline-forms graft-sym)
        data-file (-> data-file .getCanonicalPath)
        sandbox (build-sandbox pipeline-forms data-file)

        executable-code-form `(grafter.rdf.preview/preview-graph ~(list pipe-sym data-file) ~template-sym ~row)]

    (log/info "code form is" executable-code-form)

    (sandbox executable-code-form)))


;; TODO support this route without pagination
(defroutes graft-route
  (POST "/evaluate/graft" {{:keys [pipeline data command row] :as params} :params}
        (if-invalid [errors (validate-graft-run-request params)]
                    {:status 422 :body errors}
                    (if-not (empty? row)
                      {:status 200 :body (preview-graft-with-row (Integer/parseInt (trim row)) data command pipeline)}
                      {:status 200 :body (execute-graft data command pipeline)}))))

(comment

  (execute-graft-with-row 1 "/Users/rick/repos/grafter-template/resources/leiningen/new/grafter/example-data.csv" "my-graft" {:tempfile (clojure.java.io/file "/Users/rick/repos/graftwerk/test/data/example_pipeline.clj")})

)
