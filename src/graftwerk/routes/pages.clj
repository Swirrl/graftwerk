(ns graftwerk.routes.pages
  (:require [net.cgrand.reload :refer [auto-reload]]
            [net.cgrand.enlive-html :as en]
            [compojure.core :refer [defroutes GET]]))

(auto-reload *ns*)

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
                         :value "my-pipe"
                         :placeholder placeholder)))

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
  [{:keys [destination]}]
  [:form] (en/do->
           (en/set-attr :action destination)
           (en/content
            (upload-widget "pipeline"
                           :description "The pipeline specifying the transformation"
                           :advice "A Grafter transformation")
            (upload-widget "data"
                           :description "The data you wish to transform"
                           :advice "CSV or Excel formats supported")
            (text-widget "command" "Command to execute (can be a graft or a pipe)" "my-pipe")
            (numeric-widget "page" "Page number" 0 1000)
            (numeric-widget "page-size" "Page size" 0 1000)
            (submit-widget "Transform"))))

(en/deftemplate form-page html-template
  [{:keys [title description form]}]
  [:html :title] (en/content "Graftwerk")
  [:#contents :h2 en/first-child] (en/content title)
  [:#contents :section :p] (en/content description)
  [:#contents :form] (en/substitute (form-widget form)))

(defn render [content]
  (apply str content))

(defroutes page-routes
  (GET "/pipe" []
       (render (form-page {:title "Run a pipe"
                           :description "Upload a pipeline containing a grafter pipe and the tabular file you wish to transform"
                           :form {:destination "/evaluate/pipe"}})))
  (GET "/graft" []
       (render (form-page {:title "Run a graft"
                           :description "Upload a pipeline containing a grafter graft and the tabular file you wish to transform"
                           :form {:destination "/evaluate/graft"}}))))
