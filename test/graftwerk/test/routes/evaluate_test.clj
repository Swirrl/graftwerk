(ns graftwerk.routes.evaluate-test
  (:require [graftwerk.routes.evaluate :as eval] :reload)
  (:require [clojure.test :refer :all]
            [graftwerk.middleware :refer [common-api-middleware]]
            [ring.middleware.multipart-params :refer [multipart-params-request]]
            [ring.mock.request :as req]
            [grafter.tabular :refer [make-dataset]]
            [clojure.java.io :as io]))

(def app:csv "application/csv")

(def app:edn "application/clojure")

(defn get-file-request-data [path content-type]
  (let [file (io/file path)]
    {:tempfile file
     :filename (.getName file)
     :size (.length file)
     :content-type content-type}))

(defn add-multipart [request part-key part-value]
  (assoc-in request [:params part-key] part-value))

(defn evaluate-request []
  (-> (req/request :post "/evaluate/pipe")
      (add-multipart :pipeline (get-file-request-data "./test/graftwerk/example_pipeline.clj" app:edn))
      (add-multipart :data (get-file-request-data "./test/graftwerk/example-data.csv" app:csv))
      (add-multipart :command "my-pipe")))

(defn add-param [req param value]
  (assoc-in req [:params param] value))

(deftest paginate-seq-test
  (let [results (eval/paginate-seq (range 100) "10" "2")]
    (is (= '(20 21 22 23 24 25 26 27 28 29)
           results))))

(deftest paginate-test
  (let [ds (make-dataset (repeat 100 [:a :b :c]))]
    (is (= 20 (count (:rows (eval/paginate ds "20" "1")))))))

(deftest pipe-route-test
  (let [pipe-route (common-api-middleware eval/pipe-route)]
    (testing "with valid parameters"
      (let [test-request (evaluate-request)
            {:keys [status body]} (pipe-route test-request)]

        (is (= 200 status))
        (is (= [:name :sex :age :person-uri] (:column-names body)))))

    (testing "with invalid parameters"
      (let [test-request (-> (evaluate-request)
                             (add-param :page "foo"))
            response (pipe-route test-request)]

        (is (= 422 (:status response)))))))

(comment
  (deftest find-graft-test
    (is (:type )))

  (deftest graft-route-test
    ))
