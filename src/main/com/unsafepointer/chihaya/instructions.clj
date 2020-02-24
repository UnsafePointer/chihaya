(ns com.unsafepointer.chihaya.instructions
  (:require [com.unsafepointer.chihaya.bitwise :as bitwise]
            [com.unsafepointer.chihaya.screen :as screen]))

(defn jp-addr
  "1nnn - JP addr
  Jump to location nnn."
  [state nnn]
  (let [program-counter (:program-counter @state)]
    (swap! state assoc :program-counter nnn)))

(defn ld-Vx-byte
  "6xkk - LD Vx, byte
  Set Vx = kk."
  [state Vx byte]
  (let [registers (:registers @state)
        updated-registers (assoc registers Vx byte)]
    (swap! state assoc :registers updated-registers)))

(defn ld-I-addr
  "Annn - LD I, addr
  Set I = nnn."
  [state addr]
  (swap! state assoc :address-register addr))

(defn drw-Vx-Vy-nibble
  "Dxyn - DRW Vx, Vy, nibble
  Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision."
  [state Vx Vy nibble]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        updated-screen (atom (:screen @state))
        updated-registers (atom registers)
        memory (:memory @state)
        address-register (:address-register @state)]
    (loop [memory-index 0]
      (when (< memory-index nibble)
        (let [position (+ address-register memory-index)
              byte (nth memory position)
              bit-sequence (bitwise/bit-seq byte)]
          (doseq [[xIndex bit] (map-indexed vector bit-sequence)]
            (let [x (rem (+ Vx-value xIndex) 64) ; If the sprite is positioned so part of it is outside the coordinates of the display,
                  y (rem (+ Vy-value memory-index) 32) ; it wraps around to the opposite side of the screen.
                  screen-bit (screen/get-screen-pixel-value @updated-screen x y)
                  screen-bit-updated (bit-xor screen-bit bit)
                  collision? (> screen-bit screen-bit-updated)]
              (when collision?
                (swap! updated-registers assoc 0xF 0x1))
              (swap! updated-screen screen/update-screen x y screen-bit-updated))))
        (recur (inc memory-index))))
        (swap! state assoc :registers @updated-registers)
        (swap! state assoc :screen @updated-screen)))

(defn se-Vx-byte
  "3xkk - SE Vx, byte
  Skip next instruction if Vx = kk."
  [state Vx byte]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        program-counter (:program-counter @state)]
    (when (= Vx-value byte)
      (swap! state assoc :program-counter (+ program-counter 2)))))

(defn sne-Vx-byte
  "4xkk - SNE Vx, byte
  Skip next instruction if Vx != kk."
  [state Vx byte]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        program-counter (:program-counter @state)]
    (when (not= Vx-value byte)
      (swap! state assoc :program-counter (+ program-counter 2)))))

(defn se-Vx-Vy
  "5xy0 - SE Vx, Vy
  Skip next instruction if Vx = Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        program-counter (:program-counter @state)]
    (when (= Vx-value Vy-value)
      (swap! state assoc :program-counter (+ program-counter 2)))))
