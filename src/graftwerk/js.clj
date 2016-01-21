(ns graftwerk.js
  {:no-doc true}
  (:require [clj-rhino :as js])
  (:import [org.mozilla.javascript Context UniqueTag NativeArray NativeObject
            BaseFunction NativeJavaObject ConsString]
           [org.marianoguerra.rhino TimedContextFactory]))

(extend-protocol js/RhinoConvertible

  java.util.Date
  (-to-rhino [object scope ctx]
    object))


(extend-protocol js/ClojureConvertible

  org.mozilla.javascript.NativeDate
  (-from-rhino [object]
    (Context/jsToJava object java.util.Date)))

(defn- js-fn
  "Compile a javascript function and return a clojure function that
  calls it.

  Can be called with a scope to specify what is available to the javascript e.g.

  - (js/new-root-scope)
  - (js/new-safe-scope)
  - (js/new-root-scope nil true vars-to-remove)
  - (js/new-safe-root-scope)
  - (js/new-scope)
  "

  ([js-str]
   (js-fn js-str Integer/MAX_VALUE (js/new-safe-scope)))
  ([js-str timeout]
   (js-fn js-str timeout (js/new-safe-scope)))
  ([js-str timeout scope]
   (let [scope scope
         jsf (js/compile-function scope js-str)]
       (fn javascript-function [& args]
         (js/from-js (apply js/call-timeout (concat [scope jsf timeout] args)))))))

(defn js-fn-timeout
  [js-str]
  (js-fn js-str 1000))

(defn js-fn-no-timeout [js-str]
  (js-fn js-str))
