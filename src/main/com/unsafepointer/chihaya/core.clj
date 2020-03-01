(ns com.unsafepointer.chihaya.core
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:require [clojure.tools.cli :refer [parse-opts]])
  (:require [com.unsafepointer.chihaya.emulator :as emulator])
  (:require [com.unsafepointer.chihaya.screen :as screen])
  (:require [quil.core :as q])
  (:require [quil.middleware :as m]))

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

(defn setup [file-path print-instructions]
  (q/frame-rate 60)
  (q/color-mode :rgb)
  (q/no-stroke)
  (emulator/create-initial-state file-path print-instructions))

(defn update-state [state]
  (emulator/read-current-instruction state)
  (emulator/execute-next-instruction state)
  state)

(defn draw-state [state]
  (q/background 240)
  (let [cell-width (quot (q/width) screen/width)
        cell-height (quot (q/height) screen/height)
        screen (:screen @state)]
    (loop [x 0]
      (when (< x screen/width)
        (loop [y 0]
          (when (< y screen/height)
            (let [value (screen/get-screen-pixel-value screen x y)]
              (q/fill
                (if (= 1 value) 0 255))
                (q/rect (* x cell-width) (* y cell-height) cell-width cell-height))
            (recur (inc y))))
        (recur (inc x))))))

(defn on-close [state]
  (System/exit 0))

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
            (q/defsketch chihaya
              :host "host"
              :title "千早"
              :size [(* screen/width screen/scale) (* screen/height screen/scale)]
              :setup (partial setup file-path print-instructions)
              :update update-state
              :draw draw-state
              :middleware [m/fun-mode]
              :on-close on-close)))))))
