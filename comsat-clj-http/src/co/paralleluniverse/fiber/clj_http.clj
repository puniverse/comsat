; Pulsar: lightweight threads and Erlang-like actors for Clojure.
; Copyright (C) 2013-2015, Parallel Universe Software Co. All rights reserved.
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
(ns clj-http
  (:require [clj-http.client :as client]
            [clj-http.core :as core]
            [clj-http.conn-mgr :as conn]
            [clj-http.multipart :as mp]
            [clj-http.util :refer [opt]]
            [co.paralleluniverse.pulsar :as pc])
  (:import (org.apache.http.params CoreConnectionPNames)
           (org.apache.http.auth AuthScope UsernamePasswordCredentials NTCredentials)
           (org.apache.http.client.methods HttpUriRequest HttpEntityEnclosingRequestBase)
           (org.apache.http HttpResponseInterceptor HttpEntityEnclosingRequest HttpEntity HttpHost)
           (org.apache.http.entity StringEntity ByteArrayEntity)
           (java.io ByteArrayOutputStream)
           (org.apache.http.impl.nio.client DefaultHttpAsyncClient)
           (org.apache.http.client.params ClientPNames CookiePolicy)
           (org.apache.http.cookie.params CookieSpecPNames)
           (co.paralleluniverse.fibers.httpclient FiberHttpClient)
           (org.apache.http.conn.params ConnRoutePNames)
           (org.apache.http.conn.routing HttpRoute RouteInfo$LayerType)
           (org.apache.http.nio.conn ClientAsyncConnectionManager NoopIOSessionStrategy)
           (org.apache.http.impl.conn ProxySelectorRoutePlanner SystemDefaultRoutePlanner)
           (org.apache.http.impl.nio.conn PoolingNHttpClientConnectionManager)
           (org.apache.http.impl.nio.reactor DefaultConnectingIOReactor)
           (org.apache.http.config Registry RegistryBuilder)
           (org.apache.http.conn.ssl SSLContexts BrowserCompatHostnameVerifier AllowAllHostnameVerifier)
           (org.apache.http.nio.conn.ssl SSLIOSessionStrategy)
           (org.apache.http.conn.routing.RouteInfo LayerType)))

(def ^:dynamic request
  "Executes the HTTP request corresponding to the given map and returns
  the response map for corresponding to the resulting HTTP response.
  In addition to the standard Ring request keys, the following keys are also
  recognized:
  * :url
  * :method
  * :query-params
  * :basic-auth
  * :content-type
  * :accept
  * :accept-encoding
  * :as
  The following additional behaviors over also automatically enabled:
  * Exceptions are thrown for status codes other than 200-207, 300-303, or 307
  * Gzip and deflate responses are accepted and decompressed
  * Input and output bodies are coerced as required and indicated by the :as
  option."
  (client/wrap-request #'fiber-request))

(def print-debug! #'core/print-debug!)

(defn set-client-param [^DefaultHttpAsyncClient client key val]
  (when-not (nil? val)
    (-> client
        (.getParams)
        (.setParameter key val))))

(defn add-client-params!
  "Add various client params to the http-client object, if needed."
  [^DefaultHttpAsyncClient http-client kvs]
  (let [cookie-policy (:cookie-policy kvs)
        cookie-policy-name (str (type cookie-policy))
        kvs (dissoc kvs :cookie-policy)]
    (when cookie-policy
      (-> http-client
          .getCookieSpecs
          (.register cookie-policy-name (cookie-spec-factory cookie-policy))))
    (doto http-client
      (set-client-param ClientPNames/COOKIE_POLICY
                        (if cookie-policy
                          cookie-policy-name
                          CookiePolicy/BROWSER_COMPATIBILITY))
      (set-client-param CookieSpecPNames/SINGLE_COOKIE_HEADER true)
      (set-client-param ClientPNames/HANDLE_REDIRECTS false))

    (doseq [[k v] kvs]
      (set-client-param http-client
                        k (cond
                            (and (not= ClientPNames/CONN_MANAGER_TIMEOUT k)
                                 (instance? Long v))
                            (Integer. ^Long v)
                            true v)))))

(def insecure-scheme-registry
  (.. (RegistryBuilder/create)
    (register "http" NoopIOSessionStrategy/INSTANCE)
    (register "https" (SSLIOSessionStrategy. (SSLContexts/createSystemDefault) (AllowAllHostnameVerifier.)))
    build))

(def regular-scheme-registry
  (.. (RegistryBuilder/create)
    (register "http" NoopIOSessionStrategy/INSTANCE)
    (register "https" (SSLIOSessionStrategy. (SSLContexts/createSystemDefault) (BrowserCompatHostnameVerifier.)))
    build))

(defn ^Registry get-keystore-scheme-registry
  [{:keys [keystore keystore-type keystore-pass keystore-instance
           trust-store trust-store-type trust-store-pass]
    :as req}]
  (let [ks (conn/get-keystore keystore keystore-type keystore-pass)
        ts (conn/get-keystore trust-store trust-store-type trust-store-pass)
        context (.. (SSLContexts/custom) (loadKeyMaterial ks keystore-pass) (loadTrustMaterial ts) build)]
    (.. (RegistryBuilder/create)
      (register "http" NoopIOSessionStrategy/INSTANCE)
      (register "https" (SSLIOSessionStrategy. context (if (opt req :insecure) (BrowserCompatHostnameVerifier.) (AllowAllHostnameVerifier.))))
      build)))

(defn ^PoolingNHttpClientConnectionManager make-conn-manager
  [{:keys [keystore trust-store] :as req}]
  (cond
    (or keystore trust-store)
    (PoolingNHttpClientConnectionManager.
      (DefaultConnectingIOReactor.)
      (get-keystore-scheme-registry req))

    (opt req :insecure) (PoolingNHttpClientConnectionManager. (DefaultConnectingIOReactor.) insecure-scheme-registry)

    :else (PoolingNHttpClientConnectionManager. (DefaultConnectingIOReactor.) regular-scheme-registry)))

; TODO It looks like NIO doesn't support socket factories (sockets have to be created from a SocketChannel), and couldn't find how to enable support for SOCKS in any other way either (search: SocketChannel SOCKS support)
#_(defn ^PoolingNHttpClientConnectionManager make-socks-proxied-conn-manager
  "Given an optional hostname and a port, create a connection manager that's
  proxied using a SOCKS proxy."
  [^String hostname ^Integer port]
  (let [socket-factory #(conn/socks-proxied-socket hostname port)
        reg (.. (RegistryBuilder/create)
                (register "http" NoopIOSessionStrategy/INSTANCE)
                ???
                (register "https" (SSLIOSessionStrategy. (SSLContexts/createSystemDefault) (BrowserCompatHostnameVerifier.)))
                build)]
    (PoolingNHttpClientConnectionManager. (DefaultConnectingIOReactor.) reg)))

(defn maybe-force-proxy [^DefaultHttpAsyncClient client
                         ^HttpEntityEnclosingRequestBase request
                         proxy-host proxy-port proxy-ignore-hosts]
  (let [uri (.getURI request)]
    (when (and (nil? ((set proxy-ignore-hosts) (.getHost uri))) proxy-host)
      (let [target (HttpHost. (.getHost uri) (.getPort uri) (.getScheme uri))
            route (HttpRoute. target nil (HttpHost. ^String proxy-host
                                                    (int proxy-port))
                              ; TODO check
                              (if (.. client getConnectionManager getSchemeRegistry (getScheme target) getLayeringStrategy)
                                RouteInfo$LayerType/LAYERED
                                RouteInfo$LayerType/PLAIN))]
        (set-client-param client ConnRoutePNames/FORCED_ROUTE route)))
    request))

(defn- set-routing
  "Use ProxySelectorRoutePlanner to choose proxy sensible based on
  http.nonProxyHosts"
  [^DefaultHttpAsyncClient client]
  (.setRoutePlanner client
                    ; TODO check
                    (SystemDefaultRoutePlanner. nil))
  client)

(defn fiber-request
  "Executes the HTTP request corresponding to the given Ring request map and
  returns the Ring response map corresponding to the resulting HTTP response.
  Note that where Ring uses InputStreams for the request and response bodies,
  the clj-http uses ByteArrays for the bodies."
  [{:keys [request-method scheme server-name server-port uri query-string
           headers body multipart socket-timeout conn-timeout proxy-host
           proxy-ignore-hosts proxy-port proxy-user proxy-pass as cookie-store
           retry-handler response-interceptor digest-auth ntlm-auth connection-manager
           client-params]
    :as req}]
  (let [^ClientAsyncConnectionManager conn-mgr
        (or connection-manager
            conn/*connection-manager*
            (make-conn-manager req))
        ^DefaultHttpAsyncClient http-client (set-routing (DefaultHttpAsyncClient. conn-mgr))
        scheme (name scheme)]

    (when-let [cookie-store (or cookie-store core/*cookie-store*)]
      (.setCookieStore http-client cookie-store))
    ; TODO look further to see if Apache HTTP async supports some form of request retry handler
    #_(when retry-handler
      (.setHttpRequestRetryHandler
        http-client
        (proxy [HttpRequestRetryHandler] []
          (retryRequest [e cnt context]
            (retry-handler e cnt context)))))

    (add-client-params!
      http-client
      ;; merge in map of specified timeouts, to
      ;; support backward compatiblity.
      (merge {CoreConnectionPNames/SO_TIMEOUT         socket-timeout
              CoreConnectionPNames/CONNECTION_TIMEOUT conn-timeout}
             client-params))

    (when-let [[user pass] digest-auth]
      (.setCredentials
        (.getCredentialsProvider http-client)
        (AuthScope. nil -1 nil)
        (UsernamePasswordCredentials. user pass)))

    (when-let [[user password host domain] ntlm-auth]
      (.setCredentials
        (.getCredentialsProvider http-client)
        (AuthScope. nil -1 nil)
        (NTCredentials. user password host domain)))

    (when (and proxy-user proxy-pass)
      (let [authscope (AuthScope. proxy-host proxy-port)
            creds (UsernamePasswordCredentials. proxy-user proxy-pass)]
        (.setCredentials (.getCredentialsProvider http-client)
                         authscope creds)))

    (let [http-url (str scheme "://" server-name
                        (when server-port (str ":" server-port))
                        uri
                        (when query-string (str "?" query-string)))
          req (assoc req :http-url http-url)
          proxy-ignore-hosts (or proxy-ignore-hosts
                                 #{"localhost" "127.0.0.1"})
          ^HttpUriRequest http-req (maybe-force-proxy
                                     http-client
                                     (core/http-request-for request-method http-url body)
                                     proxy-host
                                     proxy-port
                                     proxy-ignore-hosts)]
      (when response-interceptor
        (.addResponseInterceptor
          http-client
          (proxy [HttpResponseInterceptor] []
            (process [resp ctx]
              (response-interceptor resp ctx)))))

      (when-not (conn/reusable? conn-mgr)
        (.addHeader http-req "Connection" "close"))

      (doseq [[header-n header-v] headers]
        (if (coll? header-v)
          (doseq [header-vth header-v]
            (.addHeader http-req header-n header-vth))
          (.addHeader http-req header-n (str header-v))))

      (if multipart
        (.setEntity ^HttpEntityEnclosingRequest http-req
                    (mp/create-multipart-entity multipart))
        (when (and body (instance? HttpEntityEnclosingRequest http-req))
          (if (instance? HttpEntity body)
            (.setEntity ^HttpEntityEnclosingRequest http-req body)
            (.setEntity ^HttpEntityEnclosingRequest http-req
                        (if (string? body)
                          (StringEntity. ^String body "UTF-8")
                          (ByteArrayEntity. body))))))

      (when (opt req :debug) (print-debug! req http-req))

      (try
        (let [fiber-client (FiberHttpClient. http-client)
              http-resp (.execute fiber-client http-req)
              http-entity (.getEntity http-resp)
              resp {:status (.getStatusCode (.getStatusLine http-resp))
                    :headers (core/parse-headers
                               (.headerIterator http-resp)
                               (opt req :use-header-maps-in-response))
                    :body (core/coerce-body-entity req http-entity conn-mgr)}]
          (if (opt req :save-request)
            (-> resp
                (assoc :request req)
                (assoc-in [:request :body-type] (type body))
                (update-in [:request]
                           #(if (opt req :debug-body)
                             (assoc % :body-content
                                      (cond
                                        (isa? (type (:body %)) String)
                                        (:body %)

                                        (isa? (type (:body %)) HttpEntity)
                                        (let [baos (ByteArrayOutputStream.)]
                                          (.writeTo ^HttpEntity (:body %) baos)
                                          (.toString baos "UTF-8"))

                                        :else nil))
                             %))
                (assoc-in [:request :http-req] http-req)
                (dissoc :save-request?))
            resp))
        (catch Throwable e
          (when-not (conn/reusable? conn-mgr)
            (conn/shutdown-manager conn-mgr))
          (throw e))))))

; TODO Set suspendables

; TODO Decide if this is a good way to go, it would theoretically let people use the original clj-http API in fiber-blocking mode after importing this ns

(alter-var-root #'client/request (constantly 'fiber-request))

; TODO Test
