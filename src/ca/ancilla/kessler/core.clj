(ns ca.ancilla.kessler.core
  (:gen-class)
  (:import java.lang.Character)
  (:use clearley.core ca.ancilla.kessler.lexer))

; an SFS is a sequence of objects and properties, optionally commented
; a comment starts with // and extends to the end of the line
; a property consists of NAME '=' VALUE, where VALUE is an arbitrary string
; running from the first non-whitespace after the '=' to the end of the line
; an object consists of TYPE '{' SFS '}', where TYPE is an allcaps NAME

(def sfs-lexer
  (lexer
    ["\\s+"           :whitespace :drop-token]
    ["//.*"           :comment    :drop-token]
    ["\\{"            :open-brace]
    ["\\}"            :close-brace]
    ["[a-zA-Z0-9]+"   :key]
    ["=\\s*(.*)"      :value      #(-> %2)]))

(defmacro deftoken [name tag]
  `(def ~name (scanner #(= (:tag %) ~tag) #(:value %))))

(deftoken sfs-key :key)
(deftoken sfs-value :value)
(deftoken open-brace :open-brace)
(deftoken close-brace :close-brace)

(defrule sfs-item
  ([sfs-key sfs-value] [sfs-key sfs-value])
  ([sfs-key open-brace sfs close-brace] { :type sfs-key :properties sfs }))

(defrule sfs
  ([sfs-item sfs] (cons sfs-item sfs))
  ([sfs-item] [sfs-item]))

(def sfs-parser (build-parser sfs))

(defn -main
  [& args]
  (println (execute sfs-parser (lex-seq sfs-lexer (slurp "test.sfs")))))
