(defproject co.paralleluniverse/comsat-clj-ring "0.3.0-SNAPSHOT"
  :description "Comsat integration for the Ring Clojure web framework."
  :url "https://github.com/puniverse/comsat"

  ; TODO Check if it works for POM generation to have two under :licenses, Leiningen's sample doesn't list this case
  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  ; TODO Check if it works to have it outside of license(s), Leiningen's sample doesn't list this case
  :distribution :repo

  :min-lein-version "2.4.3"

  :dependencies
    [[org.clojure/clojure "1.6.0"]
     [co.paralleluniverse/comsat-clj-ring-jetty9-adapter]
     [co.paralleluniverse/comsat-clj-ring-middleware]]
  :plugins
    [[lein-sub "0.3.0"]
     [lein-pprint "1.1.1"]
     [codox "0.8.10"]
     [lein-html5-docs "2.2.0"]
     [lein-midje "3.1.3"]
     [lein-marginalia "0.8.0"]]
  :sub
    ["comsat-clj-ring-jetty9-adapter"
     "comsat-clj-ring-middleware"]
  :codox
    {:src-dir-uri "http://github.com/puniverse/comsat/blob/0.3.0-SNAPSHOT/"
     :src-linenum-anchor-prefix "L"
     :sources ["comsat-clj/comsat-clj-ring/comsat-clj-ring-jetty9-adapter/src"
               "comsat-clj/comsat-clj-ring/comsat-clj-ring-middleware/src"]})
