(ns pondent.specs.time
  (:require [clojure.spec.alpha :as s]))


(def iso-8601 #"\d{4}-[0-1]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d[+-][0-2]\d:[0-5]\d")


(s/def ::date #(instance? goog.date.DateTime %))
(s/def ::date-pattern string?)
(s/def ::date-iso-8601 (s/and string? #(re-matches iso-8601 %)))
(s/def ::time-offset (s/double-in :min -24.0 :max 24.0 :NaN? false :infinite? false))


(s/fdef pondent.time/date->str :args (s/cat :date ::date :patt (s/? ::date-pattern)) :ret string?)
(s/fdef pondent.time/now :ret ::date)
(s/fdef pondent.time/str->date :args (s/cat :s ::date-iso-8601 :offset (s/? ::time-offset))) :ret (s/nilable ::date)
