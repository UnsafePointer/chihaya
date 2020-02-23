(ns com.unsafepointer.chihaya.screen)

(defn create-empty-screen []
  (vec (take 64 (repeat (vec (repeat 32 0)))))) ; 64x32-pixel monochrome display
