(defproject ca.ancilla/kessler "1.0-SNAPSHOT"
  :description "An asynchronous client-server multiplayer system for Kerbal Space Program"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-git-deps "0.0.1-SNAPSHOT"]
            [lein-marginalia "0.7.1"]]
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clearley "0.1.0-SNAPSHOT"]
                 [org.clojars.hozumi/clj-det-enc "1.0.0-SNAPSHOT"]]
  :git-dependencies [["https://github.com/mthvedt/clearley.git"]]
  :main ca.ancilla.kessler.core)
