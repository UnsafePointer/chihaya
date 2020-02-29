(ns com.unsafepointer.chihaya.binary-coded-decimal
  (:require [clojure.math.numeric-tower :as math]))

(defn bcd-seq
  "Returns the BCD representation of a number as a sequence of 4-bit numbers"
  [byte]
  (let [first-digit (fn [number digits] (quot number (math/expt 10 digits)))
        shift-first-digit (fn [number place] (rem number (math/expt 10 place)))
        input     {:q (first-digit byte 2) ; current position
                   :r (shift-first-digit byte 2)
                   :p 1} ; next position
        transform #(hash-map :q (first-digit (:r %) (:p %))
                             :r (shift-first-digit (:r %) (:p %))
                             :p (dec (:p %)))]
    (->> (take 3 (iterate transform input))
         (map :q))))
