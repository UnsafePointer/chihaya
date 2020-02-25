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

(defn add-Vx-byte
  "7xkk - ADD Vx, byte
  Set Vx = Vx + kk."
  [state Vx byte]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        result (bit-and (+ Vx-value byte) 0xFF)
        registers-updated (assoc registers Vx byte)]
    (swap! state assoc :registers registers-updated)))

(defn sne-Vx-Vy
  "9xy0 - SNE Vx, Vy
  Skip next instruction if Vx != Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        program-counter (:program-counter @state)]
    (when (not= Vx-value Vy-value)
      (swap! state assoc :program-counter (+ program-counter 2)))))

(defn call-addr
  "2nnn - CALL addr
  Call subroutine at nnn."
  [state nnn]
  (let [program-counter (:program-counter @state)
        stack (:stack @state)]
    (swap! state assoc :stack (conj stack program-counter))
    (swap! state assoc :program-counter nnn)))

(defn ret
  "00EE - RET
  Return from a subroutine."
  [state]
  (let [stack (:stack @state)
        top (peek stack)
        stack-updated (pop stack)]
    (swap! state assoc :stack stack-updated)
    (swap! state assoc :program-counter top)))

(defn ld-Vx-Vy
  "8xy0 - LD Vx, Vy
  Set Vx = Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vy-value (nth registers Vy)
        updated-registers (assoc registers Vx Vy-value)]
    (swap! state assoc :registers updated-registers)))

(defn or-Vx-Vy
  "8xy1 - OR Vx, Vy
  Set Vx = Vx OR Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        result (bit-or Vx-value Vy-value)
        updated-registers (assoc registers Vx result)]
    (swap! state assoc :registers updated-registers)))

(defn and-Vx-Vy
  "8xy2 - AND Vx, Vy
  Set Vx = Vx AND Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        result (bit-and Vx-value Vy-value)
        updated-registers (assoc registers Vx result)]
    (swap! state assoc :registers updated-registers)))

(defn xor-Vx-Vy
  "8xy3 - XOR Vx, Vy
  Set Vx = Vx XOR Vy."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        result (bit-xor Vx-value Vy-value)
        updated-registers (assoc registers Vx result)]
    (swap! state assoc :registers updated-registers)))

(defn add-Vx-Vy
  "8xy4 - ADD Vx, Vy
  Set Vx = Vx + Vy, set VF = carry."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        intermediate-result (+ Vx-value Vy-value)
        carry (if (> intermediate-result 0xFF) 0x1 0x0)
        result (bit-and intermediate-result 0xFF)
        updated-registers (atom (assoc registers Vx result))]
    (swap! updated-registers assoc 0xF carry)
    (swap! state assoc :registers @updated-registers)))

(defn sub-Vx-Vy
  "8xy5 - SUB Vx, Vy
  Set Vx = Vx - Vy, set VF = NOT borrow."
  [state Vx Vy]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        Vy-value (nth registers Vy)
        intermediate-result (- Vx-value Vy-value)
        not-borrow (if (> Vx-value Vy-value) 0x1 0x0)
        result (bit-and intermediate-result 0xFF)
        updated-registers (atom (assoc registers Vx result))]
    (swap! updated-registers assoc 0xF not-borrow)
    (swap! state assoc :registers @updated-registers)))

(defn shl-Vx
  "8xyE - SHL Vx {, Vy}
  Set Vx = Vx SHL 1."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        test (if (bit-test Vx-value 7) 1 0)
        intermediate-result (bit-shift-left Vx-value 1)
        result (bit-and intermediate-result 0xFF)
        updated-registers (atom (assoc registers Vx result))]
    (swap! updated-registers assoc 0xF test)
    (swap! state assoc :registers @updated-registers)))
