;
; COMSAT
; Copyright (C) 2014, Parallel Universe Software Co. All rights reserved.
;
; This program and the accompanying materials are dual-licensed under
; either the terms of the Eclipse Public License v1.0 as published by
; the Eclipse Foundation
;
;   or (per the licensee's choosing)
;
; under the terms of the GNU Lesser General Public License version 3.0
; as published by the Free Software Foundation.
;

;
; Based on ring.adapter.jetty in Ring (https://github.com/ring-clojure/ring)
; Copyright the original Authors (https://github.com/ring-clojure/ring/blob/master/CONTRIBUTORS.md)
; Released under the MIT license.
;

(ns ^{:author "circlespainter"} co.paralleluniverse.fiber.ring.jetty9
  "A Ring adapter that uses the Jetty 9 embedded web server in async mode and dispatches to Quasar fibers.
   Adapters are used to convert Ring handlers into running web servers."
  (:import (java.security KeyStore)
           (javax.servlet AsyncContext DispatcherType)
           (org.eclipse.jetty.server Server Request HttpConnectionFactory HttpConfiguration)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (org.eclipse.jetty.server ServerConnector)
           (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.util BlockingArrayQueue)
           (org.eclipse.jetty.util.ssl SslContextFactory))
  (:require [ring.util.servlet :as servlet]
            [co.paralleluniverse.pulsar.core :as pc]))

(def ^:private ^String async-exception-attribute-name "co.paralleluniverse.fiber.ring.jetty9.asyncException")
(def ^:private ^String writer-mode-stream-exception-message "WRITER")

(defn- fiber-async-proxy-handler
  "Returns a async fiber-dispatching Jetty Handler implementation for the given Ring handler."
  [handler]
  ; The handler will always be instrumented, assuming it will fiber-block,
  ; just like Pulsar does with fns passed to its API
  (pc/suspendable! handler)
  (proxy [AbstractHandler] []
    (handle [_ ^Request base-request request response]
      (if-let [^Throwable exc
                (and
                  (= DispatcherType/ASYNC (.getDispatcherType request))
                  (.getAttribute request async-exception-attribute-name))]
        ; CASE: dispatch because of exception in fiber's async processing
        (do
          ; 1 - Clean the request from the temporary attribute used to carry the exception across the dispatch
          (.removeAttribute request async-exception-attribute-name)
          ; 2 - Throw the exception in the servlet thread
          (throw exc))
        ; CASE: normal execution
        (let [^AsyncContext async-context (.startAsync request)]
          (pc/spawn-fiber #(try
                            (let [request-map  (servlet/build-request-map request)
                                  response-map (handler request-map)]
                              (when response-map
                                (servlet/update-servlet-response response response-map)
                                ; When writing to the stream directly, the output has to be flushed or the fiber could
                                ; end before it's been completely written (because of buffering).
                                ; Ignoring IllegalStateException as "flush" is not allowed when the stream has been put
                                ; in "writer" mode, which happens with strings
                                (try (.. response getOutputStream flush)
                                     (catch
                                       IllegalStateException ise
                                       (if (not= writer-mode-stream-exception-message (.getMessage ise)) (throw ise)))))
                              (.complete async-context)
                              (.setHandled base-request true))
                            ; Exception handling must be managed through "dispatch" and request attributes
                            (catch
                              Throwable t
                              (do
                                ; Not sure why I'm getting "wrong rumber of arguments" when calling setAttributes
                                ; on "request" rather than "base-request"
                                (.setAttribute base-request async-exception-attribute-name t)
                                (.dispatch async-context))
                              ))))))))

(defn- ssl-context-factory
  "Creates a new SslContextFactory instance from a map of options."
  [options]
  (let [context (SslContextFactory.)]
    (if (string? (options :keystore))
      (.setKeyStorePath context (options :keystore))
      (.setKeyStore context ^KeyStore (options :keystore)))
    (.setKeyStorePassword context (options :key-password))
    (cond
      (string? (options :truststore))
      (.setTrustStore context ^String (options :truststore))
      (instance? KeyStore (options :truststore))
      (.setTrustStore context ^KeyStore (options :truststore)))
    (when (options :trust-password)
      (.setTrustStorePassword context (options :trust-password)))
    (case (options :client-auth)
      :need (.setNeedClientAuth context true)
      :want (.setWantClientAuth context true)
      nil)
    context))

(defn- ssl-connector
  "Creates a SslSelectChannelConnector instance."
  [^Server server options]
  (doto (ServerConnector. server (ssl-context-factory options))
    (.setPort (options :ssl-port 443))
    (.setHost (options :host))
    (.setIdleTimeout (options :max-idle-time 200000))))

(defn- create-blocking-array-queue [^Integer capacity ^Integer grow-by & [^Integer max-capacity]]
  (if max-capacity
    (BlockingArrayQueue. capacity grow-by max-capacity)
    (BlockingArrayQueue. capacity grow-by)))

(defn- create-server
  "Construct a Jetty Server instance."
  [options]
  (let [^Integer min-threads
          (options :min-threads 5)
        ^Integer max-threads
          (options :max-threads 10)
        ^BlockingArrayQueue q
         (create-blocking-array-queue min-threads min-threads)
        ^QueuedThreadPool p
          (QueuedThreadPool. max-threads min-threads 60000 q)
        ^Server
          server (Server. p)
        ^ServerConnector connector
          (doto (ServerConnector. server)
            (.setPort (options :port 80))
            (.setHost (options :host))
            (.setIdleTimeout (options :max-idle-time 200000)))]
    (.addConnector server connector)
    (when-let [queued (options :max-queued)]
      (.setAcceptQueueSize connector queued))
    (when (:daemon? options false)
      (.setDaemon p true))
    (when (or (options :ssl?) (options :ssl-port))
      (let [connector-ssl (ssl-connector server options)]
        (when-let [queued (options :max-queued)]
          (.setAcceptQueueSize connector-ssl queued))
        (.addConnector server connector-ssl)))
    (doseq [conn-factory (.getConnectionFactories connector)]
      (if (instance? HttpConnectionFactory conn-factory)
        (let [^HttpConfiguration http-conf (.getHttpConfiguration conn-factory)]
          (.setSendDateHeader http-conf true))))
    server))

(defn ^Server run-jetty
  "Start a Jetty webserver to serve the given handler according to the
  supplied options:
  :configurator   - a function called with the Jetty Server instance
  :port           - the port to listen on (defaults to 80)
  :host           - the hostname to listen on
  :join?          - blocks the thread until server ends (defaults to true)
  :daemon?        - use daemon threads (defaults to false)
  :ssl?           - allow connections over HTTPS
  :ssl-port       - the SSL port to listen on (defaults to 443, implies :ssl?)
  :keystore       - the keystore to use for SSL connections
  :key-password   - the password to the keystore
  :truststore     - a truststore to use for SSL connections
  :trust-password - the password to the truststore
  :max-threads    - the maximum number of threads to use (default 10)
  :min-threads    - the minimum number of threads to use (default 5)
  :max-queued     - the maximum number of requests to queue (default unbounded)
  :max-idle-time  - the maximum idle time in milliseconds for a connection (default 200000)
  :client-auth    - SSL client certificate authenticate, may be set to :need,
                    :want or :none (defaults to :none)"
  [handler options]
  (let [^Server s (create-server (dissoc options :configurator))]
    (.setHandler s (fiber-async-proxy-handler handler))
    (when-let [configurator (:configurator options)]
      (configurator s))
    (try
      (.start s)
      (when (:join? options true)
        (.join s))
      s
      (catch Exception ex
        (.stop s)
        (throw ex)))))