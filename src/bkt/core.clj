(ns bkt.core
  (:require [clojure.math :as math]))

;(set! *warn-on-reflection* true)

(defn init-params-constant
  "initialize all BKT params to some value"
  [value]
  {:init value :guess value :slip value :transit value})

; multiple configurations of max bounds for parameters
(def default-bounds (init-params-constant 0.99999))
(def init-bounds (merge default-bounds {:init 0.85 :transit 0.3}))
(def guess-slip-bounds (merge default-bounds {:guess 0.3 :slip 0.1}))

; min bounds for parameters
(def min-bounds (init-params-constant 0.00001))

(defn init-params-random
  "initialize BKT parameters constrained to bounds"
  [bounds]
  (let [random-params {:init (rand) 
                       :guess (rand) 
                       :slip (rand) 
                       :transit (rand)}]
                     (merge-with * random-params bounds)))

(defn apply-bounds-to-parameters 
  "apply the min and max bounds to the BKT parameters"
  [parameters bounds]
  (as-> parameters $
    (merge-with min $ bounds)
    (merge-with max $ min-bounds)))

(defn- mean
  "Calculate the mean of a collection"
  ^double
  [^doubles coll]
  (let [^double sum (apply + coll)
        c (count coll)]
    (if (pos? c)
      (/ sum c)
      0)))

(defn rmse
  "calculate the Root Mean Square Error"
  ^double
  [^doubles predictions ^doubles actuals]
  (let [squared-difference (fn [^double prediction ^double actual] 
                             (let [difference (- actual prediction)]
                               (* difference difference)))]
    (math/sqrt (mean (map squared-difference predictions actuals)))))

(defn probability-learn-given-correct 
  "the probability of learning given correct observation"
  ^double
  [^double learn {:keys [^double slip ^double guess ^double transit]}]
  (/ (* learn (- 1.0 slip))
     (+ (* learn (- 1.0 slip)) (* (- 1.0 learn) guess))))

(defn probability-learn-given-incorrect 
  "the probability of learning given correct observation"
  [^double learn {:keys [^double slip ^double guess ^double transit]}]
  (/ (* learn slip)
     (+ (* learn slip) (* (- 1.0 learn) (- 1.0 guess)))))

(defn probability-correct 
  "probability of the correctly applying the skill next"
  ^double
  [^double learn {:keys [^double slip ^double guess]}]
  (+  
    (* learn (- 1.0 slip))
    (* (- 1.0 learn) guess)))

(defn probability-learned 
  "probability skill is learned given prior obs"
  [^double learn {:keys [^double transit]}]
  (+ learn 
     (* (- 1.0 learn) transit)))

(defn predict-next-known 
  "predict the probability we are in a learned state given answer and previous learned probability"
  ^double
  [^double learn correct? params]
  (let [plearn (if correct?
                 probability-learn-given-correct
                 probability-learn-given-incorrect)]
    (probability-learned (plearn learn params) params)))


(defn predict-known
  "predict probability of learned/known state for sequence of answers from 1 student"
  ^doubles
  [rightwrong {:keys [init] :as params}]
  (persistent!
    (reduce
      (fn [acc correct?] 
        (let [learn (nth acc (dec (count acc)))
              known (predict-next-known learn correct? params)]
          (conj! acc known))) (transient [init]) rightwrong)))

(defn predict-correct
  "predict probability of correct for sequence of answers from 1 student"
  [rightwrong params]
  (let [probability-known (predict-known rightwrong params)]
    (mapv #(probability-correct % params) (drop-last probability-known))))

(defn params-random-step 
  "take a random step with BKT parameters constrained to bounds"
  [params bounds]
  (let [step (* 2 (- (rand) 0.5) 0.05)
        adjustment #(+ step %) 
        parameters [:init :guess :slip :transit]
        random-parameter (rand-nth parameters)]
    (-> params
      (update random-parameter adjustment)
      (apply-bounds-to-parameters bounds))))

(defn- bool->int 
  "convert a boolean to an int"
  [x]
  (if x 1 0))

(defn fit-model [correct parameter-bounds]
  (let [actuals (->> correct (flatten) (map bool->int))
        predict (fn [correct params] (mapcat #(predict-correct % params) correct))]
    (loop [i 0
           current-params (init-params-random parameter-bounds)
           current-rmse (rmse actuals (predict correct current-params))
           best-rmse Double/MAX_VALUE
           best-params (init-params-constant 0.01)
           prev-best-rmse Double/MAX_VALUE
           temp 0.005]
      (let [done? (>= i 1e6)
            new-params (params-random-step current-params parameter-bounds)
            predictions (predict correct new-params)
            new-rmse (rmse actuals predictions)
            accept-move? (<= (rand) (Math/exp (/ (- current-rmse new-rmse) temp)))
            better-rmse? (< new-rmse best-rmse)
            big-step? (and (pos? i) (zero? (mod i 1e4)))
            no-improvement? (= prev-best-rmse best-rmse)]
        (if (or done?  (and big-step? no-improvement?))
          {:params best-params :iterations i :rmse best-rmse}
          (recur (inc i)
                 (if accept-move? new-params current-params)
                 (if accept-move? new-rmse current-rmse)
                 (if better-rmse? new-rmse best-rmse)
                 (if better-rmse? new-params best-params)
                 (if big-step? best-rmse prev-best-rmse)
                 (if big-step? (/ temp 2.0) temp)))))))
