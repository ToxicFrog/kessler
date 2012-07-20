(ns ca.ancilla.kessler.sfs
  (:require (ca.ancilla.kessler.sfs
    [reader :as reader]
    [writer :as writer])))

(defn read
  "Parse a string as an SFS; returns the parsed data."
  [str]
  (reader/parse str))

(defn write
  "Returns the string representation of an SFS, suitable for loading by KSP."
  [sfs]
  (writer/write sfs))

(defn load
  "Loads an SFS file with the given name from disk."
  [file]
  (->> file slurp read))

(defn save
  "Saves an SFS file to disk."
  [sfs file]
  (->> sfs write (spit file)))
