(ns com.unsafepointer.chihaya.screen
  (:require [clojure.string :as string]))

(def width 64)

(def height 32)

(def scale 10)

(defn create-empty-screen []
  (vec (take width (repeat (vec (repeat height 0)))))) ; 64x32-pixel monochrome display

(defn update-screen [screen x y bit]
  (let [row (nth screen x)
        updated-row (assoc row y bit)]
    (assoc screen x updated-row)))

(defn get-screen-pixel-value [screen x y]
  (let [row (nth screen x)]
    (nth row y)))

(defn print-screen [screen]
  (doseq [scanline screen]
    (prn (string/join scanline))))
