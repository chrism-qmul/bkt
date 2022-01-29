(ns bkt.examples
  (:require [bkt.core :refer :all]))

(time (fit-model [[false true true false true true true true] [false false true true true true true]] guess-slip-bounds))


(time (fit-model [[false false true true false false false false] [false false true false true true true true] [false false false false true]] guess-slip-bounds))
