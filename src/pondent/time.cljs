(ns pondent.time
  (:require [clojure.spec.alpha :as s]
            [goog.date.DateTime]
            [goog.i18n.DateTimeFormat]))


;; Specs

(def iso-8601 #"\d{4}-[0-1]\d-[0-3]\dT[0-2]\d:[0-5]\d:[0-5]\d[+-][0-2]\d:[0-5]\d")


(s/def ::date #(instance? goog.date.DateTime %))
(s/def ::date-pattern string?)
(s/def ::date-iso-8601 (s/and string? #(re-matches iso-8601 %)))
(s/def ::time-offset (s/double-in :min -24.0 :max 24.0 :NaN? false :infinite? false))


(s/fdef date->str :args (s/cat :date ::date :patt (s/? ::date-pattern)) :ret string?)
(s/fdef now :ret ::date)
(s/fdef str->date :args (s/cat :s ::date-iso-8601 :offset (s/? ::time-offset))) :ret (s/nilable ::date)


;; Date conversions

(defn date->str
  "Formats the date, `date`, using the default 'd MMMM yyyy, h:mm a' pattern. An
  alternative pattern, `patt`, can also be provided."
  ([date]
   (date->str date "d MMMM yyyy, h:mm a"))
  ([date patt]
   (.format (new goog.i18n.DateTimeFormat patt) date)))


(defn now
  "Returns the current time."
  []
  (goog.date.DateTime.))


(defn str->date
  "Parse a date, `s`, in ISO 8601 format and return a goog.date.DateTime object.
  An hour `offset` can also be provided that will be used to adjust the final
  object. If `s` cannot be parsed as a valid time, return `nil`."
  ([s]
   (str->date s 0))
  ([s offset]
   (let [millis (.parse js/Date s)]
     (when-not (.isNaN js/Number millis)
       (->> millis
            (+ (* offset 60 60 1000))
            (.fromTimestamp goog.date.DateTime))))))
