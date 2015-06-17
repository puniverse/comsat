(ns co.paralleluniverse.fiber.httpkit.client
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
