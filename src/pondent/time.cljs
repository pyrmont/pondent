(ns pondent.time
  (:require [goog.date.DateTime]
            [goog.i18n.DateTimeFormat]))

;; Date conversions

(defn date->str
  "Formats the date, `date`, using the default 'd MMMM yyyy, h:mm a' pattern. An
  alternative pattern, `patt`, can also be provided."
  ([date]
   (date->str date "d MMMM yyyy, h:mm a"))
  ([date patt]
   (.format (new goog.i18n.DateTimeFormat patt) date)))

(defn str->date
  "Parse a date, `s`, in ISO 8601 format and return a goog.date.DateTime object.
  An hour `offset` can also be provided that will be used to adjust the final
  object."
  ([s]
   (str->date s 0))
  ([s offset]
   (->> s
        (.parse js/Date)
        (+ (* offset 60 60 1000))
        (.fromTimestamp goog.date.DateTime))))
