(ns ca.ancilla.kessler.core
  (:gen-class)
  (:use [the.parsatron]))

(defparser instruction []
  (choice (char \>)
    (char \<)
    (char \+)
    (char \-)
    (char \.)
    (char \,)
    (between (char \[) (char \]) (many (instruction)))))

(defparser bf []
  (many (instruction))
  (eof))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!")
  (println (run (bf) (slurp "test.sfs"))))

