(defproject graftwerk "0.1.0-SNAPSHOT"

  :description "FIXME: write description"
  :url "http://example.com/FIXME"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [ring-server "0.3.1"]
                 [com.taoensso/timbre "4.0.1"]
                 [com.taoensso/tower "3.0.2"]
                 [selmer "0.8.0"]
                 [enlive "1.1.5"]
                 [grafter "0.5.0-SNAPSHOT"]
                 [clojail "1.0.6"]
                 [environ "1.0.0"]
                 [compojure "1.3.2"]
                 [grafter/vocabularies "0.1.0"]
                 [ring/ring-defaults "0.1.3"]
                 [ring/ring-session-timeout "0.1.0"]
                 [ring-middleware-format "0.4.0"]
                 [noir-exception "0.2.3"]
                 [bouncer "0.3.2"]
                 [prone "0.8.0"]]

  :min-lein-version "2.5.0"
  :uberjar-name "graftwerk.jar"

  :repl-options {:init-ns graftwerk.core
                 :init (-main)
                 :timeout 60000}

  :jvm-opts ["-server" "-Djava.security.manager" "-Djava.security.policy=/Users/rick/.java.policy"]

  :main graftwerk.core

  :resource-paths ["girder"]

  :plugins [[lein-ring "0.9.1"]
            [lein-environ "1.0.0"]
            [lein-ancient "0.6.0"]]

  :ring {:handler graftwerk.handler/app
         :init    graftwerk.handler/init
         :destroy graftwerk.handler/destroy
         :uberwar-name "graftwerk.war"}

  :profiles
  {:uberjar {:omit-source true
             :env {:production true}

             :aot :all}
   :production {:ring {:open-browser? false
                       :stacktraces?  false
                       :auto-reload?  false}}
   :dev {:dependencies [[ring-mock "0.1.5"]
                        [prismatic/schema "0.4.3"]
                        [ring/ring-devel "1.3.2"]
                        [pjstadig/humane-test-output "0.6.0"]]

         :injections [(require 'pjstadig.humane-test-output)
                      (pjstadig.humane-test-output/activate!)]
         :env {:dev true}}})
