(ns graftwerk.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [graftwerk.routes.evaluate :refer [pipe-route graft-route]]
            [graftwerk.middleware :refer [common-api-middleware
                                          development-middleware
                                          production-middleware]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [environ.core :refer [env]] :reload))

(defroutes base-routes
  (GET "/" []
       ;; Redirect to docs on github
       (redirect "https://github.com/Swirrl/graftwerk#running-pipes-and-grafts-on-the-whole-dataset"))
  (route/resources "/" {:root "build"})
  (route/not-found "Not Found"))

(defn init
  "init will be called once when app is deployed as a servlet on an
  app server such as Tomcat put any initialization code here"
  []

  (timbre/merge-config!
   {:appenders {:spit (appenders/spit-appender {:fname "graftwerk.log"})}
    })

  (timbre/info "\n-=[ graftwerk started successfully"
               (when (env :dev) "using the development profile") "]=-"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "shutdown complete!"))

(def app
  (-> (routes
       #'pipe-route
       #'graft-route
       #'base-routes)
      common-api-middleware
      development-middleware
      production-middleware))
