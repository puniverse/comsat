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

(ns ^{:author "circlespainter"} co.paralleluniverse.fiber.httpkit.client
  (:refer-clojure :exclude [get await])
  (:require [org.httpkit.client :as hc]
            [co.paralleluniverse.pulsar.core :refer [defsfn await]]))

(defsfn request [req] (await hc/request req))

(defmacro ^:private defreq [method]
  `(defsfn
     ~method
     ([^String ~'url]
       (await ~(symbol "hc" (name method)) ~'url {}))
     ([^String ~'url ~'opts]
       (await ~(symbol "hc" (name method)) ~'url ~'opts))))

(defreq get)
(defreq delete)
(defreq head)
(defreq post)
(defreq put)
(defreq options)
(defreq patch)
