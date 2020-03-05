(ns com.unsafepointer.chihaya.keyboard
  (:require [clojure.core.match :refer [match]]))

(defn keypad-index-for-key
  "Returns the corresponding key index for a key press or
  release, or nil. Key mapping is as follows:
  1 2 3 C    1 2 3 4
  4 5 6 D -> Q W E R
  7 8 9 E    A S D F
  A 0 B F    Z X C V"
  [key]
  (match key
    :1 0x1
    :2 0x2
    :3 0x3
    :4 0xC
    :q 0x4
    :w 0x5
    :e 0x6
    :r 0xD
    :a 0x7
    :s 0x8
    :d 0x9
    :f 0xE
    :z 0xA
    :x 0x0
    :c 0xB
    :v 0xF
    :else nil))

(defn update-keyboard [state key pressed]
  (let [keypad-index (keypad-index-for-key key)]
    (when keypad-index
      (let [keyboard (:keyboard @state)
            keyboard-updated (assoc keyboard keypad-index pressed)]
        (swap! state assoc :keyboard keyboard-updated)))))
