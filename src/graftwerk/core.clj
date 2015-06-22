(ns graftwerk.core
  (:require [graftwerk.handler :refer [app]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :as log])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        host (or (System/getenv "HOST") "0.0.0.0")]
    (log/info "Graftwerk started visit" (str "http://localhost:" port "/"))
    (run-jetty #'app {:port port :host host :join? false})))
