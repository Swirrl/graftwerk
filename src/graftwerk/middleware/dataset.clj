(ns graftwerk.middleware.dataset
  (:require [grafter.tabular :refer [make-dataset dataset? write-dataset]]
            [grafter.tabular.common :refer [write-dataset* dataset->seq-of-seqs]]
            [ring.middleware.format-response :refer [parse-accept-header]]
            [clojure.java.io :refer [output-stream]]
            [ring.util.mime-type :refer [default-mime-types]]
            [taoensso.timbre :as log]
            [ring.util.io :refer [piped-input-stream]]
            [clojure.data.csv :refer [write-csv]])
  (:import [java.io OutputStream]))

(defn stream-csv [output dataset]
  (let [rows (dataset->seq-of-seqs dataset)
        stringified-rows (map (partial map str) rows)]
    (write-csv output stringified-rows)))

(defmacro with-out-stream [ostream & body]
  `(binding [*out* ~ostream]
    ~@body))

(defn stream-edn [output dataset]
  (let [rows (:rows dataset)
        cols (:column-names dataset)]
    (with-out-stream output
      (prn {:column-names cols :rows rows}))))

;; It would be great if we could find a way so we don't have to do
;; this.  Essentially I'd like to use ring.middleware.format-response
;; or another middleware to negotiate the response format/type for us.
;; However the above middleware seems to assume that responses are
;; strings, where-as really we want to stream a response to the
;; client.
;;
;; I submitted a bug-report/feature-request here:
;;
;; See: https://github.com/ngrunwald/ring-middleware-format/issues/41
;;
;; NOTE: We also did something similar in drafter, so it would be good
;; to find a way to do this once and for all.
;;
;; But in the mean time I've just hacked this in, in the hope it'll be
;; simple enough and good enough; even if technically incorrect with
;; regards to content-neg.

(defn- select-content-type [accepts]
  (let [{:keys [sub-type type]}
        (->> accepts
             parse-accept-header
             (sort-by :q)
             reverse
             first)]
    ;; rebuild the object into a string... stupid I know...
    (str type "/" sub-type)))

(def ^:private mime-type->streamer {"application/csv" stream-csv
                                    "application/edn" stream-edn})

(defn- ->stream
  "Takes a dataset and a supported tabular format and returns a
  piped-input-stream to stream the dataset to the client."
  [dataset streamer]
  (piped-input-stream
   (fn [ostream]
     (try
       (with-open [writer (clojure.java.io/writer ostream)]
         (log/info "w" writer "o" ostream "ds" dataset)
         (streamer writer dataset))
       (log/info "Dataset streamed")
       (catch Exception ex
         (log/warn ex "Unexpected Exception whilst streaming dataset"))))))

(defn wrap-write-dataset [app]
  (fn write-dataset-middleware
    [req]
    (let [response (app req)]
      (if (dataset? (:body response))
        (let [dataset (:body response)
              accepts (get-in [:headers "accept"] req "application/edn")
              selected-format (select-content-type accepts)
              content-type (get ext->mime-type selected-format)
              selected-streamer (get mime-type->streamer content-type stream-edn)]

          (-> response
              (assoc :body (->stream dataset selected-streamer))
              (assoc-in [:headers "Content-Type"] content-type)))
        response))))
