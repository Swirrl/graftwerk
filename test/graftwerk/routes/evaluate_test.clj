(ns graftwerk.routes.evaluate-test
  (:require [graftwerk.routes.evaluate :refer :all])
  (:require [clojure.test :refer :all]
            [graftwerk.middleware :refer [common-api-middleware]]
            [ring.middleware.multipart-params :refer [multipart-params-request]]
            [ring.mock.request :as req]
            [schema.core :as s]
            [grafter.tabular :refer [make-dataset]]
            [clojure.java.io :as io]))

(def app:csv "application/csv")

(def app:edn "application/clojure")


(def preview-response-schema {:bindings {(s/optional-key :strs) [s/Symbol]
                                         (s/optional-key :keys) [s/Symbol]}
                              :row s/Any
                              :template [s/Any]})

(defn stub-multipart-file [file-path]
  {:tempfile (io/file file-path)})

(deftest preview-graft-test
  (is (s/validate preview-response-schema
                  (preview-graft-with-row 1  (stub-multipart-file "./test/data/example-data.csv") "my-graft" (stub-multipart-file "./test/data/example_pipeline.clj")))))


(deftest stop-hacking-attempts-test
  (is
   (try
     (preview-graft-with-row 1 (stub-multipart-file "./test/data/example-data.csv") "my-graft" (stub-multipart-file "./test/data/haxor_pipeline.clj"))
     false
     (catch java.util.concurrent.ExecutionException ex
       (is (instance? java.security.AccessControlException (.getCause ex))
           "Pipelines calling System/exit should throw a security exception")
       true)))
  "Should prevent security exploits by raising an Exception")


(defn get-file-request-data [path content-type]
  (let [file (io/file path)]
    {:tempfile file
     :filename (.getName file)
     :size (.length file)
     :content-type content-type}))

(defn add-multipart [request part-key part-value]
  (assoc-in request [:params part-key] part-value))

(defn evaluate-request [path command]
  (-> (req/request :post path)
      (add-multipart :pipeline (get-file-request-data "./test/data/example_pipeline.clj" app:edn))
      (add-multipart :data (get-file-request-data "./test/data/example-data.csv" app:csv))
      (add-multipart :command command)))

(defn add-param [req param value]
  (assoc-in req [:params param] value))

(deftest paginate-seq-test
  (let [results (paginate-seq (range 100) "10" "2")]
    (is (= '(20 21 22 23 24 25 26 27 28 29)
           results))))

(deftest paginate-test
  (let [ds (make-dataset (repeat 100 [:a :b :c]))]
    (is (= 20 (count (:rows (paginate ds "20" "1")))))))

(deftest pipe-route-test
  (let [pipe-route (common-api-middleware pipe-route)]
    (testing "with valid parameters"
      (let [test-request (evaluate-request "/evaluate/pipe" "my-pipe")
            {:keys [status body]} (pipe-route test-request)]

        (is (= 200 status))
        (is (= [:name :sex :age :person-uri] (:column-names body)))))

    (testing "with invalid parameters"
      (let [test-request (-> (evaluate-request "/evaluate/pipe" "my-pipe")
                             (add-param :page "foo"))
            response (pipe-route test-request)]

        (is (= 422 (:status response)))))))

(def quad-schema {:s s/Any :p s/Any :o s/Any :c s/Any})

(deftest graft-route-test
  (testing "/evaluate/graft"
    (let [graft-route (common-api-middleware graft-route)]
      (testing "with valid parameters"
        (let [test-request (evaluate-request "/evaluate/graft" "my-graft")
              {:keys [status body] :as response} (graft-route test-request)]

          (println "response" body)
          (is (= 200 status))
          (is (s/validate [quad-schema] body))))

      (testing "with invalid parameters"
        (let [test-request (-> (evaluate-request "/evaluate/graft" "my-graft")
                               (add-param :row "foo"))
              response (graft-route test-request)]

          (is (= 422 (:status response))))))))

(deftest graft-preview-route-test
  (testing "/evaluate/graft with ?row=n (graft previews)"
    (let [graft-route (common-api-middleware graft-route)]
      (testing "with valid parameters"
        (let [test-request (-> (evaluate-request "/evaluate/graft" "my-graft")
                               (add-param :row 0))

              {:keys [status body] :as response} (graft-route test-request)]

          (is (= 200 status)
              "Returns 200 ok")
          (is (s/validate preview-response-schema body)
              "Returns a datastructure containing the template preview, bindings, etc...")))

      (testing "with invalid parameters"

        )
      )
    )
  )
