(ns com.unsafepointer.chihaya.bitwise)

(defn bit-seq
  "Returns the MSB representation of a number as a sequence of bits"
  [byte]
  (->> (take 8 (iterate #(bit-shift-right % 1) byte))
       (map #(bit-and % 0x1))
       reverse))
