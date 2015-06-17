(defproject co.paralleluniverse/comsat-clj-http "0.4.0-SNAPSHOT"
  :description "'clj-http' Quasar integration based on 'comsat-httpclient'."
  :url "https://github.com/puniverse/comsat"
  :scm {:name "git" :url "https://github.com/puniverse/comsat"}

  :licenses [{:name "Eclipse Public License" :url "http://www.eclipse.org/legal/epl-v10.html"}
             {:name "GNU Lesser General Public License - v 3" :url "http://www.gnu.org/licenses/lgpl.html"}]

  :distribution :repo

  :min-lein-version "2.5.0"

  :dependencies
    [[org.clojure/clojure "1.6.0"]

     [http-kit "2.1.19"]
     [co.paralleluniverse/pulsar "0.7.1-SNAPSHOT"]]

  :profiles {:dev {:dependencies [
                                  [clj-http "1.1.2"]
                                  [ch.qos.logback/logback-classic "1.1.3"]
                                  [compojure "1.3.2"]
                                  [ring/ring-jetty-adapter "1.3.2"]
                                  [ring/ring-core "1.3.2"]
                                 ]}}

  :java-agents [[co.paralleluniverse/quasar-core "0.7.1-SNAPSHOT"]]

  :jvm-opts [;"-Dco.paralleluniverse.fibers.verifyInstrumentation=true"
;             "-Xdebug" "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"
            ]
;  :jvm-opts ["-Dco.paralleluniverse.pulsar.instrument.auto=all"]
)
