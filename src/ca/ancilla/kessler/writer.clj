(ns ca.ancilla.kessler.writer)

(declare sfs-content)

(defn- sfs-property
  "Stringifies a single key/value pair."
  [indent [key value]]
  (str indent key " = " value "\n"))

(defn- sfs-object
  "Stringifies a single SFS interior object."
  [indent sfs]
  (str indent (:type sfs) " {\n"
              (sfs-content (str "  " indent) sfs)
              indent "}\n"))

(defn- sfs-content
  "Stringifies the interior content of an SFS - the properties and children."
  [indent sfs]
  (str (reduce str (map (partial sfs-property indent) (:properties sfs)))
    (reduce str (map (partial sfs-object indent) (:children sfs)))))

(defn sfs-str
  "Turns a parsed SFS structure back into a string."
  [sfs]
  (sfs-content "" sfs))
