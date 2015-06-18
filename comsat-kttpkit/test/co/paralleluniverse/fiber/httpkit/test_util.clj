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

(ns co.paralleluniverse.fiber.httpkit.test-util
  (:use clojure.test)
  (:import [java.io File FileOutputStream]))

(defn- string-80k []
  (apply str (map char
                  (take (* 8 1024)                ; 80k
                        (apply concat (repeat (range (int \a) (int \z))))))))
;; [a..z]+
(def const-string                       ; 8M string
  (let [tmp (string-80k)]
    (apply str (repeat 1024 tmp))))

(defn ^File gen-tempfile
  "generate a tempfile, the file will be deleted before jvm shutdown"
  ([size extension]
     (let [tmp (doto
                   (File/createTempFile "tmp_" extension)
                 (.deleteOnExit))]
       (with-open [w (FileOutputStream. tmp)]
         (.write w ^bytes (.getBytes (subs const-string 0 size))))
       tmp)))

(defn to-int [int-str] (Integer/valueOf int-str))
