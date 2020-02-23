(ns com.unsafepointer.chihaya.screen)

(defn create-empty-screen []
  (vec (take 64 (repeat (vec (repeat 32 0)))))) ; 64x32-pixel monochrome display

(defn update-screen [screen x y bit]
  (let [row (nth screen x)
        updated-row (assoc row y bit)]
    (assoc screen x updated-row)))

(defn get-screen-pixel-value [screen x y]
  (let [row (nth screen x)]
    (nth row y)))
