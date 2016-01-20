(ns graftwerk.routes.pages
  (:require [net.cgrand.reload :refer [auto-reload]]
            [net.cgrand.enlive-html :as en]
            [compojure.core :refer [defroutes GET]]))

(defroutes page-routes
  (GET "/pipe" []
       (render (form-page {:title "Run a pipe"
                           :description "Upload a pipeline containing a grafter pipe and the tabular file you wish to transform."}

                          (form-widget {:destination "/evaluate/pipe"
                                        :command-text "Pipe to execute"
                                        :placeholder "my-pipe"}
                                       (numeric-widget "page" "Page number (optional)" 0 1000)
                                       (numeric-widget "page-size" "Page size (optional)" 0 1000)))))
  (GET "/graft" []
       (render (form-page {:title "Run a graft"
                           :description "Upload a pipeline containing a grafter graft and the tabular file you wish to transform.  If you want to preview the graft specify a row to render."}

                          (form-widget {:destination "/evaluate/graft"
                                        :command-text "Graft to execute"
                                        :placeholder "my-graft"}
                                       (numeric-widget "row" "Row to preview (optional)" 0 1000)
                                       (checkbox-widget "constants" "Render constants (optional)"))))))
