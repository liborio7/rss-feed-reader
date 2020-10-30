(ns tools)

(defn transducer-debug []
  (fn [rf]
    (let [step (atom 0)]
      (fn
        ([] (rf))
        ([result] (rf result))
        ([result input]
         (prn ">>>" @step)
         (prn "input:" input)
         (prn "<<<")
         (rf result input))))))