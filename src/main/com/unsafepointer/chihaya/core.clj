(ns com.unsafepointer.chihaya.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [com.unsafepointer.chihaya.emulator :as emulator]))

(def cli-options
  [["-i" "--instructions" "Output instructions to stdout"
    :id :print-instructions
    :default false]
   ["-h" "--help"]])

(defn print-usage [summary]
  (println "Usage: chihaya [OPTION] ... <romfile>")
  (println summary))

(defn validate-arg [file-path]
  (let [file-exists (.exists (io/file file-path))
        file-not-directory (not (.isDirectory (io/file file-path)))]
    (and file-exists file-not-directory)))

(defn -main [& args]
  (let [parsed-options (parse-opts args cli-options)
        errors (:errors parsed-options)
        options (:options parsed-options)
        arguments (:arguments parsed-options)
        summary (:summary parsed-options)]
    (if (or (not-empty errors) (:help options) (not= 1 (count arguments)))
      (print-usage summary)
      (let [file-path (first arguments)
            file-path-is-valid (validate-arg file-path)]
        (if-not file-path-is-valid
          (print-usage summary)
          (let [print-instructions (:print-instructions options)]
            (emulator/start-emulation file-path print-instructions)))))))
