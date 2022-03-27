(ns bkt.core-test
  (:require [clojure.test :refer :all]
            [bkt.core :refer :all]))

(defn ≈
  ([x y] (≈ x y 0.01))
  ([x y epsilon]
   (let [diff (Math/abs (- x y))]
     (< diff epsilon))))

;https://github.com/wlmiller/BKTSimulatedAnnealing
;
;skill                      L0      G                   S     T                   RMSE    
;META-DETERMINE-DXO         1.0E-6  1.0E-6              0.1   0.7238349282632712  0.31979617064728816
;META-DETERMINE-MIDDLE-GENE 1.0E-6  0.2338693161176577  0.1   0.09068027495862355 0.4472483664489885

(def scenario-a-responses [[false true true false true true true true] [false false true true true true true]])
(def scenario-b-responses [[false false true true false false false false] [false false true false true true true true] [false false false false true]])
(def scenario-a-params {:init 1.0E-6 :guess 1.0E-6 :slip 0.1 :transit 0.7238349282632712 :rmse 0.31979617064728816})
(def scenario-b-params {:init 1.0E-6 :guess 0.2338693161176577 :slip 0.1 :transit 0.09068027495862355 :rmse 0.4472483664489885})

(deftest miller
  (testing "Miller 2014 Examples"
    (testing "Fit Scenario A"
      (let [{:keys [params rmse]} (fit-model scenario-a-responses guess-slip-bounds)
            {:keys [init guess slip transit]} params
            {correct-init :init correct-guess :guess correct-slip :slip correct-transit :transit correct-rmse :rmse} scenario-a-params]
        (is (≈ init correct-init))
        (is (≈ guess correct-guess))
        (is (≈ slip correct-slip))
        (is (≈ transit correct-transit))
        (is (≈ rmse correct-rmse))))
    (testing "Fit Scenario B"
      (let [{:keys [params rmse]} (fit-model scenario-b-responses guess-slip-bounds)
            {:keys [init guess slip transit]} params
            {correct-init :init correct-guess :guess correct-slip :slip correct-transit :transit correct-rmse :rmse} scenario-b-params]
        (is (≈ init correct-init))
        (is (≈ guess correct-guess))
        (is (≈ slip correct-slip))
        (is (≈ transit correct-transit))
        (is (≈ rmse correct-rmse))))))
