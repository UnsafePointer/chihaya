(ns com.unsafepointer.chihaya.instructions)

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
