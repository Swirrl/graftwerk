(ns graftwerk.core
  (:require [graftwerk.handler :refer [app init]]
            [ring.adapter.jetty :refer [run-jetty]]
            [taoensso.timbre :as log])
  (:gen-class))

(defn -main [& args]
  (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))
        host (or (System/getenv "HOST") "0.0.0.0")]
    (init)
    (log/info "Graftwerk started visit" (str "http://localhost:" port "/"))
    (run-jetty #'app {:port port :host host :join? false})))
