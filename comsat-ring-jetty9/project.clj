(defproject co.paralleluniverse/comsat-ring-jetty9 "0.3.0-SNAPSHOT"
  :description "Comsat integration for the Ring Clojure web framework: Jetty 9 fiber-blocking adapter."
  :url "https://github.com/puniverse/comsat"

  ; TODO Check if it works for POM generation to have two under :licenses, Leiningen's sample doesn't list this case
  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  ; TODO Check if it works to have it outside of license(s), Leiningen's sample doesn't list this case
  :distribution :repo

  :min-lein-version "2.4.3"

  :dependencies
    [[org.clojure/clojure "1.6.0"]

     [ring/ring-core "1.3.2"]
     [ring/ring-devel "1.3.2"]
     [ring/ring-servlet "1.3.2"]

     [org.eclipse.jetty/jetty-server "9.2.5.v20141112"]

     [org.slf4j/slf4j-simple "1.7.7"]

     [co.paralleluniverse/pulsar "0.6.1"]]

  :java-agents [[co.paralleluniverse/quasar-core "0.6.1"]]

  :profiles
    {:dev {:dependencies [[clj-http "1.0.1"]]}})
