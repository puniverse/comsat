; Pulsar: lightweight threads and Erlang-like actors for Clojure.
; Copyright (C) 2015, Parallel Universe Software Co. All rights reserved.
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
; Tests are ported from http-kit (http://www.http-kit.org/).
; Copyright © 2012-2013 Feng Shen.
; Distributed under the Apache License Version 2.0.
;

(ns ^{:author "circlespainter"} co.paralleluniverse.fiber.httpkit.client-test
  (:use clojure.test
        [ring.adapter.jetty :only [run-jetty]]
        [org.httpkit.server :only [run-server]]
        co.paralleluniverse.fiber.httpkit.test-util
        [clojure.string :only [split]]
        (compojure [core :only [defroutes GET PUT PATCH DELETE POST HEAD
                                DELETE ANY context]]
                   [handler :only [site]]
                   [route :only [not-found]])
        (clojure.tools [logging :only [info warn]]))
  (:require [co.paralleluniverse.fiber.httpkit.client :as http]
            [co.paralleluniverse.pulsar.core :refer [fiber]]
            [org.httpkit.client :as http-orig]
            [clj-http.client :as clj-http])
  (:import java.nio.ByteBuffer
           [org.httpkit HttpMethod HttpStatus HttpVersion]
           [org.httpkit.client Decoder IRespListener]
           (java.io InputStream FileInputStream)))

(defroutes test-routes
  (GET "/get" [] "hello world")
  (POST "/post" [] "hello world")
  (ANY "/204" [] {:status 204})
  (ANY "/redirect" [] (fn [req]
                        (let [total (-> req :params :total to-int)
                              n (-> req :params :n to-int)
                              code (to-int (or (-> req :params :code) "302"))]
                          (if (>= n total)
                            {:status 200 :body (-> req :request-method name)}
                            {:status code
                             :headers {"location" (str "redirect?total=" total "&n=" (inc n)
                                                       "&code=" code)}}))))
  (POST "/multipart" [] (fn [req] {:status 200}))
  (PATCH "/patch" [] "hello world")
  (POST "/nested-param" [] (fn [req] (pr-str (:params req))))
  (ANY "/method" [] (fn [req]
                      (let [m (:request-method req)]
                        {:status 200
                         :headers {"x-method" (pr-str m)}})))
  (ANY "/unicode" [] (fn [req] (-> req :params :str)))
  (DELETE "/delete" [] "deleted")
  (ANY "/ua" [] (fn [req] ((-> req :headers) "user-agent")))
  (GET "/keep-alive" [] (fn [req] (-> req :params :id)))
  (GET "/length" [] (fn [req]
                      (let [l (-> req :params :length to-int)]
                        (subs const-string 0 l))))
  (GET "/multi-header" [] (fn [req]
                            {:status 200
                             :headers {"x-method" ["value1", "value2"]
                                       "x-method2" ["value1", "value2", "value3"]}}))
  (GET "/p" [] (fn [req] (pr-str (:params req))))
  (ANY "/params" [] (fn [req] (-> req :params :param1)))
  (PUT "/body" [] (fn [req] {:body (:body req)
                            :status 200
                            :headers {"content-type" "text/plain"}}))
  (GET "/test-header" [] (fn [{:keys [headers]}] (str (get headers "test-header")))))

(use-fixtures :once
  (fn [f]
    (let [server (run-server (site test-routes) {:port 4347})
          jetty (run-jetty (site test-routes) {:port 14347
                                               :join? false
                                               :ssl-port 9898
                                               :ssl? true
                                               :key-password "123456"
                                               :keystore "test-resources/ssl_keystore"})]
      (try (f) (finally (server) (.stop jetty))))))

(comment
  (defonce server1 (run-server (site test-routes) {:port 4347})))

(deftest test-http-client
  (doseq [host ["http://127.0.0.1:4347" "http://127.0.0.1:14347"]]
    (is (= 200 (:status @(fiber (http/get (str host "/get"))))))
    (is (= 404 (:status @(fiber (http/get (str host "/404"))))))
    (is (= 200 (:status @(fiber (http/post (str host "/post"))))))
    (is (= 200 (:status @(fiber (http/patch (str host "/patch"))))))
    (is (= 200 (:status @(fiber (http/delete (str host "/delete"))))))
    (is (= 200 (:status @(fiber (http/head (str host "/get"))))))
    (is (= 200 (:status @(fiber (http/post (str host "/post"))))))
    (is (= 404 (:status @(fiber (http/get (str host "/404"))))))
    (is (= 200 (:status @(fiber (http/get (str host "/get"))))))
    (is (= 404 (:status @(fiber (http/get (str host "/404"))))))
    (is (= 204 (:status @(fiber (http/get (str host "/204"))))))
    (let [url (str host "/get")]
      (doseq [_ (range 0 10)]
        (let [requests (doall (map (fn [u] (fiber (http/get u))) (repeat 20 url)))]
          (doseq [r requests]
            (is (= 200 (:status @r))))))
      (doseq [_ (range 0 200)]
        (is (= 200 (:status @(fiber (http/get url)))))))))


(deftest test-unicode-encoding
  (let [u "高性能HTTPServer和Client"
        url "http://127.0.0.1:4347/unicode"
        url1 (str url "?str=" (http-orig/url-encode u))
        url2 (str "http://127.0.0.1:4347/unicode?str=" (http-orig/url-encode u))]
    (is (= u (:body @(fiber (http/get url1)))))
    (is (= u (:body (clj-http/get url1))))
    (is (= u (:body @(fiber (http/post url {:form-params {:str u}})))))
    (is (= u (:body (clj-http/post url {:form-params {:str u}}))))
    (is (= u (:body @(fiber (http/get url2)))))
    (is (= u (:body (clj-http/get url2))))))

(defn- rand-keep-alive []
  {:headers {"Connection" (cond  (> (rand-int 10) 5) "Close"
                                 :else "keep-alive")}})

(deftest test-keep-alive-does-not-messup
  (let [url "http://127.0.0.1:4347/keep-alive?id="]
    (doseq [id (range 0 100)]
      (is (= (str id) (:body @(fiber (http/get (str url id) (rand-keep-alive)))))))
    (doseq [ids (partition 10 (range 0 300))]
      (let [requests (doall (map (fn [id]
                                   (fiber (http/get (str url id) (rand-keep-alive))))
                                 ids))]
        (doseq [r requests]
          (is (= 200 (:status @r))))))))

(deftest test-http-client-user-agent
  (let [ua "test-ua"
        url "http://127.0.0.1:4347/ua"]
    (is (= ua (:body @(fiber (http/get url {:user-agent ua})))))
    (is (= ua (:body @(fiber (http/post url {:user-agent ua})))))))

(deftest test-query-string
  (let [p1 "this is a test"
        query-params {:query-params {:param1 p1}}]
    (is (= p1 (:body @(fiber (http/get "http://127.0.0.1:4347/params" query-params)))))
    (is (= p1 (:body @(fiber (http/post "http://127.0.0.1:4347/params" query-params)))))
    (is (= p1 (:body @(fiber (http/get "http://127.0.0.1:4347/params?a=b" query-params)))))
    (is (= p1 (:body @(fiber (http/post "http://127.0.0.1:4347/params?a=b" query-params)))))))

(deftest test-jetty-204-decode-properly
  ;; fix #52
  (is (= 204 (:status @(fiber (http/get "http://127.0.0.1:14347/204" {:timeout 20})))))
  (is (= 204 (:status @(fiber (http/post "http://127.0.0.1:14347/204" {:timeout 20}))))))

(deftest test-http-client-form-params
  (let [url "http://127.0.0.1:4347/params"
        value "value"]
    (is (= value (:body @(fiber (http/post url {:form-params {:param1 value}})))))))

(deftest test-http-client-async
  (let [url "http://127.0.0.1:4347/params"
        p @(fiber (http/post url {:form-params {:param1 "value"}}))]
    (is (= 200 (:status p)))
    (is (= "value" (:body p))))) ;; wait

(deftest test-max-body-filter
  (is (:error @(fiber (http/get "http://127.0.0.1:4347/get"
                                ;; only accept response's length < 3
                                {:filter (http-orig/max-body-filter 3)}))))
  (is (:status @(fiber (http/get "http://127.0.0.1:4347/get" ; should ok
                                 {:filter (http-orig/max-body-filter 30000)})))))

(deftest test-http-method
  (doseq [m [:get :post :put :delete :head]]
    (is (= m (-> @(fiber (http/request {:method m
                                        :url "http://127.0.0.1:4347/method"}))
                 :headers :x-method read-string)))))

(deftest test-string-file-inputstream-body []
  (let [length (+ (rand-int (* 1024 5)) 100)
        file (gen-tempfile length ".txt")
        bodys [(subs const-string 0 length)    ;string
               file                            ;file
               (FileInputStream. file) ; inputstream
               [(subs const-string 0 100) (subs const-string 100 length)] ; seqable
               (ByteBuffer/wrap (.getBytes (subs const-string 0 length))) ; byteBuffer
               ]]
    (doseq [body bodys]
      (is (= length (count (:body @(fiber (http/put "http://127.0.0.1:4347/body"
                                                    {:body body})))))))))

(deftest test-params
  (let [url "http://a.com/biti?wvr=5&topnav=1&wvr=5&mod=logo#ccc"
        params (-> @(fiber (http/get "http://127.0.0.1:4347/p"
                                     {:query-params {:try false :rt "url" :to 1 :u url}}))
                   :body read-string)]
    (is (= url (:u params)))
    (is (= "false" (:try params)))))

(deftest test-output-coercion
  (let [url "http://localhost:4347/length?length=1024"]
    (let [body (:body @(fiber (http/get url {:as :text})))]
      (is (string? body))
      (is (= 1024 (count body))))
    (let [body (:body @(fiber (http/get url)))] ; auto
      (is (string? body)))
    (let [body (:body @(fiber (http/get url {:as :auto})))] ; auto
      (is (string? body)))
    (let [body (:body @(fiber (http/get url {:as :stream})))]
      (is (instance? InputStream body)))
    (let [body (:body @(fiber (http/get url {:as :byte-array})))]
      (is (= 1024 (alength body))))))

(deftest test-https
  (let [get-url (fn [length] (str "https://localhost:9898/length?length=" length))]
    (doseq [_ (range 0 2)]
      (doseq [length (repeatedly 40 (partial rand-int (* 4 1024)))]
        (let [{:keys [body error _]} @(fiber (http/get (get-url length) {:insecure? true}))]
          (if error (.printStackTrace error))
          (is (= length (count body)))))
      (doseq [length (repeatedly 40 (partial rand-int (* 4 1024)))]
        (is (= length (-> @(fiber (http/get (get-url length)
                                            {:insecure? true :keepalive -1}))
                          :body count))))
      (doseq [length (repeatedly 40 (partial rand-int (* 4 1024)))]
        (is (= length (-> @(fiber (http/get (get-url length)
                                            (assoc (rand-keep-alive) :insecure? true)))
                          :body count)))))))

;; https://github.com/http-kit/http-kit/issues/54
(deftest test-nested-param
  (let [url "http://localhost:4347/nested-param"
        params {:card {:number "4242424242424242" :exp_month "12"}}]
    (is (= params (read-string (:body @(fiber (http/post url {:form-params params}))))))
    (is (= params (read-string (:body @(fiber (http/post
                                               url
                                               {:form-params {"card[number]" 4242424242424242
                                                              "card[exp_month]" 12}}))))))
    (is (= params (read-string (:body (clj-http/post url {:form-params params})))))))

(deftest test-redirect
  (let [url "http://localhost:4347/redirect?total=5&n=0"]
    (is (:error @(fiber (http/get url {:max-redirects 3})))) ;; too many redirects
    (is (= 200 (:status @(fiber (http/get url {:max-redirects 6})))))
    (is (= 302 (:status @(fiber (http/get url {:follow-redirects false})))))
    (is (= "get" (:body @(fiber (http/post url {:as :text}))))) ; should switch to get method
    (is (= "post" (:body @(fiber (http/post (str url "&code=307") {:as :text}))))) ; should not change method
  ))

(deftest test-multipart
  (is (= 200 (:status @(fiber (http/post "http://localhost:4347/multipart"
                                         {:multipart [{:name "comment" :content "httpkit's project.clj"}
                                                      {:name "file" :content (clojure.java.io/file "project.clj") :filename "project.clj"}]}))))))

(deftest test-coerce-req
  "Headers should be the same regardless of multipart"
  (let [coerce-req #'org.httpkit.client/coerce-req
        request {:basic-auth ["user" "pass"]}]
    (is (= (keys (:headers (coerce-req request)))
           (remove #(= % "Content-Type")
                   (keys (:headers (coerce-req (assoc request :multipart [{:name "foo" :content "bar"}])))))))))

(deftest test-header-multiple-values
  (let [resp @(fiber (http/get "http://localhost:4347/multi-header" {:headers {"foo" ["bar" "baz"], "eggplant" "quux"}}))
        resp2 (clj-http/get "http://localhost:4347/multi-header" {:headers {"foo" ["bar" "baz"], "eggplant" "quux"}})]
    (is (= 200 (:status resp)))
    (is (= 3 (count (split (-> resp :headers :x-method2) #","))))
    (is (= 2 (count (split (-> resp :headers :x-method) #","))))
    (is (= 200 (:status resp2)))))

(deftest test-headers-stringified
  (doseq [[sent expected] [["test" "test"]
                           [0 "0"]
                           ['(0) "0"]
                           ['("a" "b") "a,b"]]]
    (let [received (:body @(fiber (http/get "http://localhost:4347/test-header"
                                            {:headers {"test-header" sent}})))]
        (is (= received expected)))))

(defn- utf8 [s] (ByteBuffer/wrap (.getBytes s "UTF-8")))

(defn- decode
  [method buffer]
  (let [out (atom [])
        listener (reify IRespListener
                   (onInitialLineReceived [_ v s] (swap! out conj [:init v s]))
                   (onHeadersReceived [_ hs]      (swap! out conj [:headers hs]))
                   (onBodyReceived [_ buf n]      (swap! out conj [:body (into [] (take n buf))]))
                   (onCompleted [_]               (swap! out conj [:completed]))
                   (onThrowable [_ t]             (swap! out conj [:error t])))]
    (.decode (Decoder. listener method) buffer)
    @out))

(deftest test-decode-partial-status-line
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; The Status-Line is only parsed once there is a CRLF in the end.
    HttpMethod/GET "" []
    HttpMethod/GET "HTTP/1.1" []
    HttpMethod/GET "HTTP/1.1 200 OK" []
    HttpMethod/GET "HTTP/1.1totally-broken-line" []))

(deftest test-decode-http-version
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; HTTP version string is parsed.
    HttpMethod/GET "HTTP/1.1 200 OK\r\n" [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]]
    HttpMethod/GET "HTTP/1.0 200 OK\r\n" [[:init HttpVersion/HTTP_1_0 HttpStatus/OK]]))

(deftest test-decode-empty-reason-phrase
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; The Reason-Phrase (after Status-Code) may be omitted.
    HttpMethod/GET "HTTP/1.1 200 \r\n" [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]]

    ;; A Status-Line with no space after the Status-Code does not comply to the RFC 2616,
    ;; but there is probably little reason not to allow it in the parser.
    HttpMethod/GET "HTTP/1.1 200\r\n" [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]]))

(deftest test-decode-empty-headers
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; Empty headers
    HttpMethod/GET "HTTP/1.1 200 OK\r\n\r\n" [[:init HttpVersion/HTTP_1_1 HttpStatus/OK] [:headers {}]]
    HttpMethod/GET "HTTP/1.1 200 \r\n\r\n" [[:init HttpVersion/HTTP_1_1 HttpStatus/OK] [:headers {}]]))

(deftest test-decode-headers
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; Headers are not emitted before two consecutive CRLF's
    HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n"
    [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]]

    ;; One header.
    HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n"
    [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
     [:headers {"content-length" "0"}]]))

(deftest test-decode-body
  (are [method resp events] (= (decode method (utf8 resp)) events)
    ;; Empty body with zero content-length, no matter what bytes follow.
    HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 0\r\n\r\n..."
    [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
     [:headers {"content-length" "0"}]]

    ;; Expecting one byte, but no content available yet.
    HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 1\r\n\r\n"
    [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
     [:headers {"content-length" "1"}]]

     ;; One byte.
     HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 1\r\n\r\n."
     [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
      [:headers {"content-length" "1"}]
      [:body [46]]]

     ;; One byte. The rest is ignored.
     HttpMethod/GET "HTTP/1.1 200 OK\r\nContent-Length: 1\r\n\r\n..."
     [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
      [:headers {"content-length" "1"}]
      [:body [46]]]

    ;; The body is omitted for HEAD requests.
    HttpMethod/HEAD "HTTP/1.1 200 OK\r\nContent-Length: 3\r\n\r\n..."
    [[:init HttpVersion/HTTP_1_1 HttpStatus/OK]
     [:headers {"content-length" "3"}]]))

; Too long

#_(defmacro bench [title & forms]
  `(let [start# (. System (nanoTime))]
     ~@forms
     (println (str ~title "Elapsed time: "
                   (/ (double (- (. System (nanoTime)) start#)) 1000000.0)
                   " msecs"))))

#_(deftest ^:benchmark performance-bench
  (let [url "http://127.0.0.1:14347/get"]
    ;; just for fun
    (bench "http-kit, concurrency 1, 3000 requests: "
           (doseq [_ (range 0 3000)] @(fiber (http/get url))))
    (bench "clj-http, concurrency 1, 3000 requests: "
           (doseq [_ (range 0 3000)] (clj-http/get url)))
    (bench "http-kit, concurrency 10, 3000 requests: "
           (doseq [_ (range 0 300)]
             (let [requests (doall (map (fn [u] (fiber (http/get u)))
                                        (repeat 10 url)))]
               (doseq [r requests] @r))))) ; wait all finish
  (let [url "https://127.0.0.1:9898/get"]
    (bench "http-kit, https, concurrency 1, 1000 requests: "
           (doseq [_ (range 0 1000)] @(fiber (http/get url {:insecure? true}))))
    (bench "http-kit, https, concurrency 10, 1000 requests: "
           (doseq [_ (range 0 100)]
             (let [requests (doall (map (fn [u] (fiber (http/get u {:insecure? true})))
                                        (repeat 10 url)))]
               (doseq [r requests] @r)))) ; wait all finish
    (bench "clj-http, https, concurrency 1, 1000 requests: "
           (doseq [_ (range 0 1000)] (clj-http/get url {:insecure? true})))
    (bench "http-kit, https, keepalive disabled, concurrency 1, 1000 requests: "
           (doseq [_ (range 0 1000)] @(fiber (http/get url {:insecure? true
                                                            :keepalive -1}))))))

