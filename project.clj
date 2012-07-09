(defproject ca.ancilla/kessler "1.0-SNAPSHOT"
  :description "An asynchronous client-server multiplayer system for Kerbal Space Program"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :plugins [[lein-git-deps "0.0.1-SNAPSHOT"]]
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :git-dependencies [["https://github.com/mthvedt/clearley.git"]]
  :source-paths ["src" ".lein-git-deps/clearley/src"]
  :main ca.ancilla.kessler.core)
