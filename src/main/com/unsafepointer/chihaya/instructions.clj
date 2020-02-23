(ns com.unsafepointer.chihaya.instructions)

(defn jp-addr
  "1nnn - JP addr
  Jump to location nnn."
  [state nnn]
  (let [program-counter (:program-counter @state)]
    (swap! state assoc :program-counter nnn)))
