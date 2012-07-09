(ns ca.ancilla.kessler.core
  (:gen-class)
  (:import java.lang.Character)
  (:use clearley.core ca.ancilla.kessler.lexer))

; an SFS is a sequence of objects and properties, optionally commented
; a comment starts with // and extends to the end of the line
; a property consists of NAME '=' VALUE, where VALUE is an arbitrary string
; running from the first non-whitespace after the '=' to the end of the line
; an object consists of TYPE '{' SFS '}', where TYPE is an allcaps NAME

;(def not-eol (scanner #(not= \newline %) identity))

;(def rest-of-line (one-or-more not-eol))

;(def name-char (scanner #(.isLetterOrDigit %) identity))

;(def name (one-or-more name-char))

;(def key (one-or-more name))

;(defrule line
;  ([rest-of-line \newline] rest-of-line))

;(def sfs-seq (one-or-more line))

;(defrule sfs
;  ([sfs-seq] sfs-seq))

(defmacro deftoken [token-name token-p]
  `(def ~token-name (one-or-more (scanner ~token-p identity))))

(deftoken sfs-value (partial not= \newline))
(deftoken sfs-key #(Character/isLetterOrDigit %))
(deftoken sfs-type #(Character/isUpperCase %))

(defrule sfs-item
  ([sfs-type \{ sfs \}] '(typename sfs))
  ([sfs-type \= sfs-value] '(key value)))

(defrule sfs
  ([sfs \n sfs-item] (concat sfs sfs-item))
  ([sfs-item] sfs-item))

(def sfs-parser (build-parser sfs))

(def sfs-lexer
  (lexer
    ["\\s+"           :whitespace :drop-token]
    ["//.*"           :comment    :drop-token]
    ["\\{"            :open-brace]
    ["\\}"            :close-brace]
    ["[A-Z]+"         :type]
    ["[a-zA-Z0-9]+"   :key]
    ["=\\s*(.*)"      :value      #(-> %2)]))

(defn -main
  [& args]
  (dorun (map (partial println "TOKEN") (lex-seq sfs-lexer (slurp "test.sfs")))))
;  (execute sfs-parser (slurp "test.sfs")))
;  (execute sfs-parser (lex-seq lexicon (slurp "test.sfs"))))
