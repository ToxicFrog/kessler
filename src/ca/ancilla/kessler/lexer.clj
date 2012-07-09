(ns ca.ancilla.kessler.lexer
  (:use clojure.string))

(defn- lex-token
  "Creates and returns the token for the next token in input according to lexer. Throws an exception if input does not match any tokens."
  [lexer input]
  (let [try-lex (fn [lexeme] (re-find (:pattern lexeme) input))
        matching (first (filter try-lex (:lexicon lexer)))]
    (if matching
      (let [groups (re-find (:pattern matching) input)
            groups (if (string? groups) [groups] groups)]
        ;(println "token:" (:tag matching) (apply (:result matching) groups))
        (assoc matching
          :text (first groups)
          :value (apply (:result matching) groups)
          :length (count (first groups))
          :col (:col lexer)
          :line (:line lexer)))
      (throw (RuntimeException. (str "Lex error at " (:line lexer) "." (:col lexer)))))))

(defn- advance-cursor
  "Given a token, returns the [line col] of the first character *after* the token, ie, the start of the next token."
  [token]
  (let [lines (split (:text token) #"\n" -1)
        line-delta (- (count lines) 1)
        chars (last lines)
        col-delta (count chars)]
    (if (= line-delta 0)
      [(:line token) (+ (:col token) col-delta)]
      [(+ (:line token) line-delta) (inc col-delta)])))

(defn- lex-next
  "Processes the next token in input according to lexer and returns a vector of [token lexer remaining], where token is the token processed, lexer is the updated state of the lexer, and remaining is the rest of the input."
  [lexer input]
  (if (= 0 (count input)) nil
    (let [token (lex-token lexer input)
          input (subs input (:length token))
          [next-line next-col] (advance-cursor token)
          lexer (assoc lexer :line next-line :col next-col)]
      (if (= (:value token) :drop-token)
        (recur lexer input)
        [token lexer input]))))

(defn lexer []
  { :line 1 :col 1
    :lexicon [{:pattern #"^s"} {:pattern #"^."}] })

(defn lex-seq
  "Returns a lazy sequence of the tokens contained in input according to lexer. Throws an exception if lexing fails."
  [lexer input]
  (let [[lex-head lexer lex-tail] (lex-next lexer input)]
    (lazy-seq (cons lex-head (lex-seq lexer lex-tail)))))

(defn lexer
  "Given any number of [pattern tag result] lexeme descriptions, constructs a lexer from them.

  Patterns are strings describing regular expressions. Actual regexes are not permitted as the regexes will be modified by the lexer constructor.
  Tags are values used to identify the token type, typically keywords such as :operator or :number.
  Result is the value associated with this token when it is lexed, under the following rules:
  - if the keyword :drop-token, tokens of that type are silently dropped when lexing
  - if unspecified, the matched text is the result
  - if a function, it is the passed the text of the token, followed by the vector of captures (if any), and its return value is the result
  - otherwise, the value is taken as the result

  The resulting lexer can be run with (lex-seq lexer input)"
  [& lexicon]
  (let [make-pattern (fn [p] (re-pattern (str "^" p)))
        make-result (fn [r] (cond (nil? r) identity (fn? r) r :else (constantly r)))
        make-lexeme (fn [pattern tag & [result]]
                       { :pattern (make-pattern pattern) :tag tag :result (make-result result)})
        lexicon (map (partial apply make-lexeme) lexicon)]
    { :lexicon lexicon :line 1 :col 1 }))
