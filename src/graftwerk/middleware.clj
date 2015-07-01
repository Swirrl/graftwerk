(ns graftwerk.middleware
  (:require [taoensso.timbre :as timbre]
            [environ.core :refer [env]]
            [selmer.middleware :refer [wrap-error-page]]
            [prone.middleware :refer [wrap-exceptions]]
            [grafter.tabular :refer [make-dataset dataset?]]
            [ring.util.response :refer [redirect]]
            [graftwerk.middleware.dataset :refer [wrap-write-dataset]]
            [ring.middleware.defaults :refer [api-defaults wrap-defaults]]
            [ring.middleware.session-timeout :refer [wrap-idle-session-timeout]]
            [noir-exception.core :refer [wrap-internal-error]]
            [ring.middleware.format :refer [wrap-restful-format]]))

(def middleware-params (assoc-in api-defaults [:params :multipart] true))

(defn log-request [handler]
  (fn [req]
    (timbre/debug req)
    (let [{:keys [status headers] :as response} (handler req)]
      (timbre/info status " " (:request-method req) " " (:uri req) " " headers)
      response)))

(defn common-api-middleware [handler]
  (-> handler
      (wrap-defaults middleware-params)))

(defn development-middleware [handler]
  (if (env :dev)
    (-> handler
        wrap-error-page
        wrap-exceptions
        log-request)
    handler))

(defn production-middleware [handler]
  (-> handler
      wrap-write-dataset
      (wrap-idle-session-timeout
        {:timeout (* 60 30)
         :timeout-response (redirect "/")})
      (wrap-internal-error :log #(timbre/error %))
      log-request))
