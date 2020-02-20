(ns com.unsafepointer.chihaya.emulator
  (:require [clojure.java.io :as io])
  (:import  [org.apache.commons.io IOUtils]))

(defn read-rom [file-path]
  (IOUtils/toByteArray (io/input-stream file-path)))

(defn execute-next-instruction [state]
  (prn "Executing next instruction"))

(defn start-emulation [file-path]
  (let [rom (read-rom file-path)]
    (while true
      (->> {:ROM rom}
        execute-next-instruction))))
