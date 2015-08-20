(def quasar-pulsar-version "0.7.2")

(defproject co.paralleluniverse/comsat-httpkit "0.5.0-SNAPSHOT"
  :description "'httpkit' Quasar integration"

  :url "https://github.com/puniverse/comsat"

  :scm {:name "git" :url "https://github.com/puniverse/comsat"}

  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  :distribution :repo

  :min-lein-version "2.5.0"

  :dependencies
    [[org.clojure/clojure "1.6.0"]
     [http-kit "2.1.19"]
     [co.paralleluniverse/pulsar ~quasar-pulsar-version]]

  :profiles {:dev {:dependencies
    [[clj-http "1.1.2"]
     [ch.qos.logback/logback-classic "1.1.3"]
     [compojure "1.3.2"]
     [ring/ring-jetty-adapter "1.3.2"]
     [ring/ring-core "1.3.2"]]}}

  :java-agents [[co.paralleluniverse/quasar-core ~quasar-pulsar-version]]
)
