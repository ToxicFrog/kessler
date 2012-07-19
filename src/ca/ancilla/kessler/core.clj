(ns ca.ancilla.kessler.core
  (:gen-class)
  (:import java.lang.Character)
  (:use
    clearley.core
    [ca.ancilla.kessler parser writer]))

(defn -main
  [& args]
  (-> "test.sfs" slurp sfs-parse sfs-str doall println))
