(ns graftwerk.handler
  (:require [compojure.core :refer [defroutes routes GET]]
            [graftwerk.routes.evaluate :refer [pipe-route graft-route]]
            [graftwerk.routes.pages :refer [page-routes]]
            [graftwerk.middleware :refer [common-api-middleware
                                          development-middleware
                                          production-middleware]]
            [compojure.route :as route]
            [ring.util.response :refer [redirect]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [environ.core :refer [env]] :reload :verbose))

(defroutes base-routes
  (GET "/" []
       (redirect "/pipe"))
  (route/resources "/images" {:root "src/test-support/images"})
  (route/resources "/" {:root "build"})
  (route/not-found "Not Found"))

(defn init
  "init will be called once when app is deployed as a servlet on an
  app server such as Tomcat put any initialization code here"
  []
  (timbre/set-config!
    [:appenders :rotor]
    {:min-level :info
     :enabled? true
     :async? false ; should be always false for rotor
     :max-message-per-msecs nil
     :fn rotor/appender-fn})

  (timbre/set-config!
   [:shared-appender-config :rotor]
   {:path "graftwerk.log" :max-size (* 512 1024) :backlog 10})

  (timbre/info "\n-=[ graftwerk started successfully"
               (when (env :dev) "using the development profile") "]=-"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "shutdown complete!"))

(def app
  (-> (routes
       pipe-route
       graft-route
       page-routes
       base-routes)
      common-api-middleware
      development-middleware
      production-middleware))
