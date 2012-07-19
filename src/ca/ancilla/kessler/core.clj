(ns ca.ancilla.kessler.core
  (:gen-class)
  (:import java.lang.Character)
  (:use
    clearley.core
    [ca.ancilla.kessler parser writer]))

(defn -main
  [& args]
  (println (execute sfs-parser (lex-seq sfs-lexer (slurp "test.sfs")))))
