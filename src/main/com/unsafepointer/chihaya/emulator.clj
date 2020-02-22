(ns com.unsafepointer.chihaya.emulator
  (:require [clojure.java.io :as io]
            [clojure.core.match :refer [match]])
  (:import  [org.apache.commons.io IOUtils]))

(defn read-rom [file-path]
  (IOUtils/toByteArray (io/input-stream file-path)))

(defn create-initial-state [file-path]
  (let [bytes (read-rom file-path)
        rom (map #(Byte/toUnsignedInt %) bytes)
        memory (vec (concat (vec (repeat 0x200 0)) ; 0x000 to 0x1FF is reserved for the interpreter
                            (seq rom) ; 0x200 is the start of most Chip-8 programs
                            (vec (repeat (- 0xFFF (count rom) 0x200) 0)))) ; 0xFFF is the end of Chip-8 RAM
        state (atom {:memory memory
                     :program-counter  0x200
                     :current-instruction nil})]
    state))

(defn read-current-instruction [state]
  (let [program-counter (:program-counter @state)
        memory (:memory @state)
        instruction (+ (bit-shift-left (nth memory program-counter) 8)
                                       (nth memory (inc program-counter)))]
    (swap! state assoc :current-instruction instruction)
    (swap! state assoc :program-counter (+ program-counter 2)))) ; TODO: Check when to increase PC

(defn execute-next-instruction [state]
  (let [instruction (:current-instruction @state)
        [_ Vx-character Vy-character _ :as opcode] (vec (format "%04X" instruction))]
    (match opcode
      :else (throw (Exception. (str "Unhandled operation code: " opcode))))))

(defn start-emulation [file-path]
  (let [state (create-initial-state file-path)]
    (while true
      (read-current-instruction state)
      (execute-next-instruction state))))
