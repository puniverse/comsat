(def quasar-pulsar-version "0.7.6")

(defproject co.paralleluniverse/comsat-ring-jetty9 "0.8.0-SNAPSHOT"
  :description "Comsat integration for the Ring Clojure web framework: Jetty 9 fiber-blocking adapter."
  :url "https://github.com/puniverse/comsat"
  :scm {:name "git" :url "https://github.com/puniverse/comsat"}

  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  :distribution :repo

  :dependencies
    [[org.clojure/clojure "1.7.0"]

     [ring/ring-core "1.4.0"]
     [ring/ring-devel "1.4.0"]
     [ring/ring-servlet "1.4.0"]

     [org.eclipse.jetty/jetty-server "9.3.8.v20160314"]

     [org.slf4j/slf4j-simple "1.7.21"]

     [co.paralleluniverse/pulsar ~quasar-pulsar-version]]

  :repositories [["snapshots" "http://oss.sonatype.org/content/repositories/snapshots"]]

  :java-agents [[co.paralleluniverse/quasar-core ~quasar-pulsar-version]]

  :profiles
    {:dev {:dependencies [[clj-http "2.1.0"]]}})
