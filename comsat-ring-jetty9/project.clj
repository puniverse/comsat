(defproject co.paralleluniverse/comsat-ring-jetty9 "0.4.0"
  :description "Comsat integration for the Ring Clojure web framework: Jetty 9 fiber-blocking adapter."
  :url "https://github.com/puniverse/comsat"
  :scm {:name "git" :url "https://github.com/puniverse/comsat"}

  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  :distribution :repo

  :min-lein-version "2.5.0"

  :dependencies
    [[org.clojure/clojure "1.6.0"]

     [ring/ring-core "1.3.2"]
     [ring/ring-devel "1.3.2"]
     [ring/ring-servlet "1.3.2"]

     [org.eclipse.jetty/jetty-server "9.3.0.v20150612"]

     [org.slf4j/slf4j-simple "1.7.12"]

     [co.paralleluniverse/pulsar "0.7.2"]]

  :java-agents [[co.paralleluniverse/quasar-core "0.7.2"]]

  :profiles
    {:dev {:dependencies [[clj-http "1.1.2"]]}})
