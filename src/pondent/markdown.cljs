(ns pondent.markdown
  (:require [clojure.string :as str]))

;; Character counts

(defn meta-match?
  "Return true if the two arguments represent enclosing Markdown characters."
  [open close]
  (cond
    (nil? open)   false
    (= "**" open) (= "**" close)
    (= "__" open) (= "__" close)
    (= "*" open)  (= "*" close)
    (= "_" open)  (= "_" close)
    (= "`" open)  (= "`" close)
    (= "[" open)  (str/ends-with? close ")")
    :else false))

(defn meta-length
  "Return the number of characters to be counted."
  [open close]
  (if (not (meta-match? open close))
    nil
    (if (str/starts-with? close "](")
      23 ;; This is the Twitter short link maximum length
      0)))

(defn next-plain-char
  "Return the position of the next unambiguously plain character in `text`
  starting at `start`."
  [text start]
  (let [remainder (subs text start)]
    (cond
      (str/starts-with? remainder "**") (+ 2 start)
      (str/starts-with? remainder "__") (+ 2 start)
      (str/starts-with? remainder "*")  (+ 1 start)
      (str/starts-with? remainder "_")  (+ 1 start)
      (str/starts-with? remainder "`")  (+ 1 start)
      (str/starts-with? remainder "[")  (+ 1 start)
      (str/starts-with? remainder "](") (if-let [next-pos (str/index-of text ")" start)]
                                          (+ 1 next-pos)
                                          start)
      :else start)))

(defn num-plain-chars
  "Calculate the number of plain characters."
  [text]
  (let [limit (count text)]
    (loop [i 0, length 0, meta-stack []]
      (if (< i limit)
        (let [next-pos (next-plain-char text i)]
          (if (= i next-pos) ;; The current character is a plain character
            (recur (inc i) (inc length) meta-stack)
            (let [meta-chars (subs text i next-pos)]
              (if-let [extra-length (meta-length (peek meta-stack) meta-chars)]
                (recur next-pos (+ extra-length length) (pop meta-stack))
                (recur next-pos length (conj meta-stack meta-chars))))))
        (+ length (reduce #(+ %1 (count %2)) 0 meta-stack))))))

(defn chars-left
  "Calculate the number of characters left."
  [text max-chars]
  (- max-chars (num-plain-chars text)))
