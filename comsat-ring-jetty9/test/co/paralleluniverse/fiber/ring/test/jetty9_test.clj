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
; Based on ring.adapter.test.jetty in Ring (https://github.com/ring-clojure/ring)
; Copyright the original Authors (https://github.com/ring-clojure/ring/blob/master/CONTRIBUTORS.md)
; Released under the MIT license.
;

(ns ^{:author "circlespainter"} co.paralleluniverse.fiber.ring.test.jetty9-test
  (:use clojure.test
        co.paralleluniverse.fiber.ring.jetty9)
  (:require [clj-http.client :as http])
  (:import (org.eclipse.jetty.util.thread QueuedThreadPool)
           (org.eclipse.jetty.server Server Request)
           (org.eclipse.jetty.server.handler AbstractHandler)
           (co.paralleluniverse.fibers Fiber)))

(defn- hello-world [request]
  (Fiber/sleep 100)
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn- content-type-handler [content-type]
  (constantly
    {:status  200
     :headers {"Content-Type" content-type}
     :body    ""}))

(defn- echo-handler [request]
  (Fiber/sleep 100)
  {:status 200
   :headers {"request-map" (str (dissoc request :body))}
   :body (:body request)})

(defn- exception-throwing-handler [_]
  (throw (RuntimeException. "testExc")))

(defn- all-threads []
  (.keySet (Thread/getAllStackTraces)))

(defmacro with-server [app options & body]
  `(let [server# (run-jetty ~app ~(assoc options :join? false))]
     (try
       ~@body
       (finally (.stop server#)))))

(deftest test-run-jetty
  (testing "HTTP server"
    (with-server hello-world {:port 4347}
                 (let [response (http/get "http://localhost:4347")]
                   (is (= (:status response) 200))
                   (is (.startsWith (get-in response [:headers "content-type"])
                                    "text/plain"))
                   (is (= (:body response) "Hello World")))))

  (testing "HTTP server exception"
    (with-server exception-throwing-handler {:port 4347}
                 (let [response (http/get "http://localhost:4347" {:throw-exceptions false})]
                   (is (= (:status response) 500))
                   (is (.contains
                         (:body response)
                         "testExc")))))

  (testing "HTTPS server"
    (with-server hello-world {:port 4347
                              :ssl-port 4348
                              :keystore "test/keystore.jks"
                              :key-password "password"}
                 (let [response (http/get "https://localhost:4348" {:insecure? true})]
                   (is (= (:status response) 200))
                   (is (= (:body response) "Hello World")))))

  (testing "HTTPS server exception"
    (with-server exception-throwing-handler {:port 4347
                                             :ssl-port 4348
                                             :keystore "test/keystore.jks"
                                             :key-password "password"}
                 (let [response (http/get "https://localhost:4348" {:insecure? true :throw-exceptions false})]
                   (is (= (:status response) 500))
                   (is (.contains
                         (:body response)
                         "testExc")))))

  (testing "configurator set to run last"
    (let [new-handler  (proxy [AbstractHandler] []
                         (handle [_ ^Request base-request request response]))
          configurator (fn [server]
                         (.setHandler server new-handler))
          server (run-jetty hello-world
                            {:join? false :port 4347 :configurator configurator})]
      (is (identical? new-handler (.getHandler server)))
      (is (= 1 (count (.getHandlers server))))
      (.stop server)))

  (testing "setting daemon threads"
    (testing "default (daemon off)"
      (let [^Server server (run-jetty hello-world {:port 4347 :join? false})]
        (is (not (.. server getThreadPool isDaemon)))
        (.stop server)))
    (testing "daemon on"
      (let [^Server server (run-jetty hello-world {:port 4347 :join? false :daemon? true})]
        (is (.. server getThreadPool isDaemon))
        (.stop server)))
    (testing "daemon off"
      (let [^Server server (run-jetty hello-world {:port 4347 :join? false :daemon? false})]
        (is (not (.. server getThreadPool isDaemon)))
        (.stop server))))

  (testing "setting max idle timeout"
    (let [server (run-jetty hello-world {:port 4347
                                         :ssl-port 4348
                                         :keystore "test/keystore.jks"
                                         :key-password "password"
                                         :join? false
                                         :max-idle-time 5000})
          connectors (.getConnectors server)]
      (is (= 5000 (.getIdleTimeout (first connectors))))
      (is (= 5000 (.getIdleTimeout (second connectors))))
      (.stop server)))

  (testing "using the default max idle time"
    (let [server (run-jetty hello-world {:port 4347
                                         :ssl-port 4348
                                         :keystore "test/keystore.jks"
                                         :key-password "password"
                                         :join? false})
          connectors (.getConnectors server)]
      (is (= 200000 (.getIdleTimeout (first connectors))))
      (is (= 200000 (.getIdleTimeout (second connectors))))
      (.stop server)))

  (testing "setting min-threads"
    (let [server (run-jetty hello-world {:port 4347
                                         :min-threads 3
                                         :join? false})
          thread-pool (.getThreadPool server)]
      (is (= 3 (.getMinThreads thread-pool)))
      (.stop server)))

  (testing "default min-threads"
    (let [^Server server (run-jetty hello-world {:port 4347
                                                 :join? false})
          thread-pool (.getThreadPool server)]
      (is (= 5 (.getMinThreads thread-pool)))
      (.stop server)))

  (testing "setting max-queued"
    (let [server (run-jetty hello-world {:port 4347
                                         :max-queued 7
                                         :join? false})]
      (doseq [connector (.getConnectors server)]
        (is (= 7 (.getAcceptQueueSize connector))))
      (.stop server)))

  (testing "default character encoding"
    (with-server (content-type-handler "text/plain") {:port 4347}
                 (let [response (http/get "http://localhost:4347")]
                   (is (.contains
                         (get-in response [:headers "content-type"])
                         "text/plain")))))

  (testing "custom content-type"
    (with-server (content-type-handler "text/plain;charset=UTF-16;version=1") {:port 4347}
                 (let [response (http/get "http://localhost:4347")]
                   (let [response (http/get "http://localhost:4347")]
                     (is (= (get-in response [:headers "content-type"])
                            "text/plain;charset=UTF-16;version=1")))))

    (testing "request translation"
      (with-server echo-handler {:port 4347}
                   (let [response (http/post "http://localhost:4347/foo/bar/baz?surname=jones&age=123" {:body "hello"})]
                     (is (= (:status response) 200))
                     (is (= (:body response) "hello"))
                     (let [request-map (read-string (get-in response [:headers "request-map"]))]
                       (is (= (:query-string request-map) "surname=jones&age=123"))
                       (is (= (:uri request-map) "/foo/bar/baz"))
                       (is (= (:content-length request-map) 5))
                       (is (= (:character-encoding request-map) "UTF-8"))
                       (is (= (:request-method request-map) :post))
                       (is (= (:content-type request-map) "text/plain; charset=UTF-8"))
                       (is (= (:remote-addr request-map) "127.0.0.1"))
                       (is (= (:scheme request-map) :http))
                       (is (= (:server-name request-map) "localhost"))
                       (is (= (:server-port request-map) 4347))
                       (is (= (:ssl-client-cert request-map) nil))))))

    (testing "resource cleanup on exception"
      (with-server hello-world {:port 4347}
                   (let [thread-count (count (all-threads))]
                     (is (thrown? Exception (run-jetty hello-world {:port 4347})))
                     (is (= thread-count (count (all-threads)))))))))
