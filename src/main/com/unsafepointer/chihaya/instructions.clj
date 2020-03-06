(ns com.unsafepointer.chihaya.instructions
  (:require [com.unsafepointer.chihaya.bitwise :as bitwise]
            [com.unsafepointer.chihaya.screen :as screen]
            [com.unsafepointer.chihaya.binary-coded-decimal :as bcd]))

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
        registers-updated (assoc registers Vx result)]
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

(defn shr-Vx
  "8xy6 - SHR Vx {, Vy}
  Set Vx = Vx SHR 1."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        test (if (bit-test Vx-value 0) 1 0)
        intermediate-result (bit-shift-right Vx-value 1)
        result (bit-and intermediate-result 0xFF)
        updated-registers (atom (assoc registers Vx result))]
    (swap! updated-registers assoc 0xF test)
    (swap! state assoc :registers @updated-registers)))

(defn ld-I-Vx
  "Fx55 - LD [I], Vx
  Store registers V0 through Vx in memory starting at location I."
  [state Vx]
  (let [registers (:registers @state)
        address-register (:address-register @state)
        updated-memory (atom (:memory @state))]
    (loop [index 0]
      (when (<= index Vx)
        (let [address (+ address-register index)
              value (nth registers index)]
          (swap! updated-memory assoc address value)
          (recur (inc index)))))
    (swap! state assoc :memory @updated-memory)))

(defn ld-Vx-I
  "Fx65 - LD Vx, [I]
  Read registers V0 through Vx from memory starting at location I."
  [state Vx]
  (let [memory (:memory @state)
        address-register (:address-register @state)
        updated-registers (atom (:registers @state))]
    (loop [index 0]
      (when (<= index Vx)
        (let [address (+ address-register index)
              value (nth memory address)]
          (swap! updated-registers assoc index value)
          (recur (inc index)))))
    (swap! state assoc :registers @updated-registers)))

(defn ld-B-Vx
  "Fx33 - LD B, Vx
  Store BCD representation of Vx in memory locations I, I+1, and I+2."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        address-register (:address-register @state)
        bcd-sequence (bcd/bcd-seq Vx-value)
        updated-memory (atom (:memory @state))]
    (doseq [[index digit] (map-indexed vector bcd-sequence)]
      (swap! updated-memory assoc (+ address-register index) digit))
    (swap! state assoc :memory @updated-memory)))

(defn add-I-Vx
  "Fx1E - ADD I, Vx
  Set I = I + Vx."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        memory (:memory @state)
        address-register (:address-register @state)
        address-register-value (nth memory address-register)
        result (bit-and (+ Vx-value address-register-value) 0xFFFF)]
    (swap! state assoc :address-register result)))

(defn cls
  "00E0 - CLS
  Clear the display."
  [state]
  (let [empty-screen (screen/create-empty-screen)]
    (swap! state assoc :screen empty-screen)))

(defn skp-Vx
  "Ex9E - SKP Vx
  Skip next instruction if key with the value of Vx is pressed."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)
        keyboard (:keyboard @state)
        pressed (nth keyboard Vx-value)]
    (when pressed
      (let [program-counter (:program-counter @state)]
        (swap! state assoc :program-counter (+ program-counter 2))))))

(defn ld-DT-Vx
  "Fx15 - LD DT, Vx
  Set delay timer = Vx."
  [state Vx]
  (let [registers (:registers @state)
        Vx-value (nth registers Vx)]
    (swap! state assoc :delay-timer-register Vx-value)))

(defn ld-Vx-DT
  "Fx07 - LD Vx, DT
  Set Vx = delay timer value."
  [state Vx]
  (let [registers (:registers @state)
        DT (:delay-timer-register @state)
        updated-registers (assoc registers Vx DT)]
    (swap! state assoc :registers updated-registers)))
