(ns com.unsafepointer.chihaya.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [com.unsafepointer.chihaya.emulator :as emulator]))

(defn -main [& args]
  (let [file-path (or (first args) "")
        file-exists (.exists (io/file file-path))
        file-not-directory (not (.isDirectory (io/file file-path)))]
    (if (and file-exists file-not-directory)
      (prinln "All good!")
      (println "Incorrect argument passed. See README.md for usage."))))
