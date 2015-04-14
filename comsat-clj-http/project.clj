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

     [clj-http "1.1.0"]
     [co.paralleluniverse/comsat-httpclient "0.4.0-SNAPSHOT"]
     [co.paralleluniverse/pulsar "0.6.3-SNAPSHOT"]]

  :java-agents [[co.paralleluniverse/quasar-core "0.6.3-SNAPSHOT"]])
