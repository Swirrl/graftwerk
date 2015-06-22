(ns graftwerk.routes.pages
  (:require [net.cgrand.reload :refer [auto-reload]]
            [net.cgrand.enlive-html :as en]
            [compojure.core :refer [defroutes GET]]))

;(auto-reload *ns*)

(def html-template "build/test/graftwerk.html")

(en/defsnippet numeric-widget "src/html/templates/includes/input_number.hbs"  [:.form_group]
  [id description min max]
  [:label] (en/content description)
  [:input] (en/set-attr :id id
                        :name id
                        :min min
                        :max max))

(en/defsnippet text-widget "src/html/templates/includes/input_text.hbs"  [:.form_group]
  [id description placeholder]
  [:label] (en/do->
            (en/set-attr :for id)
            (en/content description))
  [:input] (en/do->
            (en/set-attr :id id
                         :name id
                         :value placeholder
                         :placeholder placeholder)))

(en/defsnippet checkbox-widget "src/html/templates/includes/input_checkboxes.hbs" [:.form_group [:.checkbox (en/nth-child 2)]]
  [id description]
  [:span] (en/content description)
  [:input] (en/set-attr :id id
                        :name id))

(en/defsnippet submit-widget html-template [[:input (en/attr= :type "submit")]]
  [name]
  [:input] (en/set-attr :value name))

(en/defsnippet upload-widget html-template [:#contents :form [:.form_group en/first-of-type]]
  [id & {:keys [description advice]}]
  [:label] (en/do->
            (en/content description)
            (en/set-attr :for id))
  [:input] (en/do->
            (en/set-attr :id id :name id))
  [:p] (en/content advice))

(en/defsnippet form-widget html-template [:#contents :form]
  [{:keys [destination command-text placeholder]} & more-contents]
  [:form] (en/do->
           (en/set-attr :action destination)
           (apply en/content
                  (upload-widget "pipeline"
                                 :description "The pipeline specifying the transformation"
                                 :advice "A Grafter transformation")
                  (upload-widget "data"
                                 :description "The data you wish to transform"
                                 :advice "CSV or Excel formats supported")
                  (text-widget "command" command-text placeholder)

                  (concat more-contents
                          [(submit-widget "Transform")]))))

(en/deftemplate form-page html-template
  [{:keys [title description]} form]
  [:html :title] (en/content "Graftwerk")
  [:#contents :h2] (en/content title)
  [:#contents :form] (en/do->
                      (en/substitute form)
                      (en/prepend (en/html [:p description])))
  [:footer :.rhs :img] (en/set-attr :src
                                    "/images/swirrl_r.png")
  [:footer :.lhs :p] (en/html-content "&copy; 2015 Swirrl IT Limited."))

(defn render [content]
  (apply str content))

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
