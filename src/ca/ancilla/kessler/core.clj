(ns ca.ancilla.kessler.core
  (:gen-class)
  (:require [ca.ancilla.kessler.sfs :as sfs]))

(defn -main
  [& args]
  (println (sfs/write (sfs/load (first args)))))
