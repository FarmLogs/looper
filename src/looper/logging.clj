(ns looper.logging)

(defn- try-resolve
  "Tries to require and resolve the given namespace-qualified symbol, returning nil if not found."
  [sym]
  (try
    (require (symbol (namespace sym)))
    (resolve sym)
    (catch java.io.FileNotFoundException _)
    (catch RuntimeException _)))

(defmacro println-warn [& args]
  `(println "WARN:" ~@args))

(def ^:macro warn
  "Prints a warning messsage, either via timbre, tools.logging, or println, depnding on what is available"
  (or
    (try-resolve 'taoensso.timbre/warn)
    (try-resolve 'clojure.tools.logging/warn)
    #'println-warn))
