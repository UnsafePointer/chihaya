(ns com.unsafepointer.chihaya.emulator
  (:require [clojure.java.io :as io]
            [clojure.core.match :refer [match]]
            [com.unsafepointer.chihaya.instructions :as instructions]
            [com.unsafepointer.chihaya.screen :as screen])
  (:import  [org.apache.commons.io IOUtils]))

(defn read-rom [file-path]
  (IOUtils/toByteArray (io/input-stream file-path)))

(defn create-initial-state [file-path print-instructions font-resource]
  (let [rom-bytes (read-rom file-path)
        rom (map #(Byte/toUnsignedInt %) rom-bytes)
        font-bytes (read-rom font-resource)
        font (map #(Byte/toUnsignedInt %) font-bytes)
        memory (vec (concat (seq font) ; Chip-8 font starts at 0x000
                            (vec (repeat (- 0x200 (count font)) 0)) ; 0x000 to 0x1FF is reserved for the interpreter
                            (seq rom) ; 0x200 is the start of most Chip-8 programs
                            (vec (repeat (- 0xFFF (count rom) 0x200) 0)))) ; 0xFFF is the end of Chip-8 RAM
        screen (screen/create-empty-screen)
        state (atom {:memory memory
                     :program-counter  0x200
                     :current-instruction nil
                     :stack ()
                     :registers (vec (repeat 16 0))
                     :address-register nil
                     :delay-timer-register 0
                     :sound-timer-register 0
                     :screen screen
                     :print-instructions print-instructions
                     :keyboard (vec (repeat 16 false))})]
    state))

(defn update-delay-timer-register [state]
  (let [delay-timer-register (:delay-timer-register @state)]
    (when (> delay-timer-register 0)
      (swap! state assoc :delay-timer-register (- delay-timer-register 1)))))

(defn update-sound-timer-register [state]
  (let [sound-timer-register (:sound-timer-register @state)]
    (when (> sound-timer-register 0) ; TODO: As long as ST's value is greater than zero, the Chip-8 buzzer will sound.
      (swap! state assoc :sound-timer-register (- sound-timer-register 1)))))

(defn read-current-instruction [state]
  (let [program-counter (:program-counter @state)
        memory (:memory @state)
        instruction (+ (bit-shift-left (nth memory program-counter) 8)
                                       (nth memory (inc program-counter)))]
    (swap! state assoc :current-instruction instruction)
    (swap! state assoc :program-counter (+ program-counter 2)))) ; TODO: Check when to increase PC

(defn execute-next-instruction [state]
  (let [instruction (:current-instruction @state)
        [_ Vx-character Vy-character _ :as opcode] (vec (format "%04X" instruction))
        nnn (bit-and instruction 0xFFF)
        Vx (Integer/parseInt (str Vx-character) 16)
        Vy (Integer/parseInt (str Vy-character) 16)
        kk (bit-and instruction 0xFF)
        nibble (bit-and instruction 0xF)]
    (when (:print-instructions @state)
      (println (str "PC: " (:program-counter @state) " OPCODE: " opcode)))
    (match opcode
      [\0\0\E\0] (instructions/cls state)
      [\0\0\E\E] (instructions/ret state)
      [\1 _ _ _] (instructions/jp-addr state nnn)
      [\2 _ _ _] (instructions/call-addr state nnn)
      [\3 _ _ _] (instructions/se-Vx-byte state Vx kk)
      [\4 _ _ _] (instructions/sne-Vx-byte state Vx kk)
      [\5 _ _\0] (instructions/se-Vx-Vy state Vx Vy)
      [\6 _ _ _] (instructions/ld-Vx-byte state Vx kk)
      [\7 _ _ _] (instructions/add-Vx-byte state Vx kk)
      [\8 _ _\0] (instructions/ld-Vx-Vy state Vx Vy)
      [\8 _ _\1] (instructions/or-Vx-Vy state Vx Vy)
      [\8 _ _\2] (instructions/and-Vx-Vy state Vx Vy)
      [\8 _ _\3] (instructions/xor-Vx-Vy state Vx Vy)
      [\8 _ _\4] (instructions/add-Vx-Vy state Vx Vy)
      [\8 _ _\5] (instructions/sub-Vx-Vy state Vx Vy)
      [\8 _ _\6] (instructions/shr-Vx state Vx)
      [\8 _ _\E] (instructions/shl-Vx state Vx)
      [\9 _ _\0] (instructions/sne-Vx-Vy state Vx Vy)
      [\A _ _ _] (instructions/ld-I-addr state nnn)
      [\C _ _ _] (instructions/rnd-Vx-kk state Vx kk)
      [\D _ _ _] (instructions/drw-Vx-Vy-nibble state Vx Vy nibble)
      [\E _\9\E] (instructions/skp-Vx state Vx)
      [\E _\A\1] (instructions/sknp-Vx state Vx)
      [\F _\0\7] (instructions/ld-Vx-DT state Vx)
      [\F _\1\5] (instructions/ld-DT-Vx state Vx)
      [\F _\1\8] (instructions/ld-ST-Vx state Vx)
      [\F _\1\E] (instructions/add-I-Vx state Vx)
      [\F _\2\9] (instructions/ld-F-Vx state Vx)
      [\F _\3\3] (instructions/ld-B-Vx state Vx)
      [\F _\5\5] (instructions/ld-I-Vx state Vx)
      [\F _\6\5] (instructions/ld-Vx-I state Vx)
      :else (throw (Exception. (str "Unhandled operation code: " opcode))))))
