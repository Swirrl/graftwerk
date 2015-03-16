(ns graftwerk.handler
  (:require [compojure.core :refer [defroutes routes]]
            [graftwerk.middleware
             :refer [development-middleware production-middleware]]
            [graftwerk.routes.pages :refer [page-routes]]
            [graftwerk.middleware :refer [common-api-middleware
                                          development-middleware
                                          production-middleware]]
            [compojure.route :as route]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.rotor :as rotor]
            [environ.core :refer [env]]))

(defroutes base-routes
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

  (if (env :dev) (parser/cache-off!))
  ;;start the expired session cleanup job

  (timbre/info "\n-=[ graftwerk started successfully"
               (when (env :dev) "using the development profile") "]=-"))

(defn destroy
  "destroy will be called when your application
   shuts down, put any clean up code here"
  []
  (timbre/info "shutdown complete!"))

(def app
  (-> (routes
       page-routes
       base-routes)
      common-api-middleware
      development-middleware
      production-middleware))
