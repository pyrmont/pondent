(ns pondent.markdown
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]))


;; Specs

(def url-regex #"https?://[\w.-]+[\w./%#?&=-]+")


(s/def ::length #(not (neg-int? %)))
(s/def ::meta-length #{0 23})
(s/def ::url (s/and string? #(re-matches url-regex %)))


(s/fdef meta-match? :args (s/cat :open (s/nilable string?) :close string?) :ret boolean?)
(s/fdef meta-length :args (s/cat :open (s/nilable string?) :close string?) :ret (s/nilable ::meta-length))
(s/fdef next-plain-char :args (s/cat :text string? :start ::length) :ret ::length)
(s/fdef num-plain-chars :args (s/cat :text string?) :ret ::length)
(s/fdef chars-left :args (s/cat :text string? :max-chars int?) :ret int?)
(s/fdef image-ref :args (s/cat :url ::url :alt-text (s/? string?)) :ret string?)
(s/fdef link-ref :args (s/cat :url ::url :link-text (s/? string?)) :ret string?)
(s/fdef ref-pattern :args (s/cat :url ::url) :ret regexp?)


;; Character counts

(defn meta-match?
  "Return true if `open` and `close` represent enclosing Markdown characters."
  [open close]
  (cond
    (nil? open)   false
    (= "**" open) (= "**" close)
    (= "__" open) (= "__" close)
    (= "*" open)  (= "*" close)
    (= "_" open)  (= "_" close)
    (= "`" open)  (= "`" close)
    (= "[" open)  (string/ends-with? close ")")
    (= "![" open) (string/ends-with? close ")")
    :else false))


(defn meta-length
  "Return the number of characters to be counted."
  [open close]
  (if (not (meta-match? open close))
    nil
    (if (string/starts-with? close "](")
      23 ;; This is the Twitter short link maximum length
      0)))


(defn next-plain-char
  "Return the position of the next unambiguously plain character in `text`
  starting at `start`."
  [text start]
  (let [remainder (subs text start)]
    (cond
      (string/starts-with? remainder "**") (+ 2 start)
      (string/starts-with? remainder "__") (+ 2 start)
      (string/starts-with? remainder "*")  (+ 1 start)
      (string/starts-with? remainder "_")  (+ 1 start)
      (string/starts-with? remainder "`")  (+ 1 start)
      (string/starts-with? remainder "[")  (+ 1 start)
      (string/starts-with? remainder "![") (+ 2 start)
      (string/starts-with? remainder "](") (if-let [next-pos (string/index-of text ")" start)]
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


;; Links

(defn image-ref
  "Get the reference for an image."
  ([url]
   (image-ref url "alt text"))
  ([url alt-text]
   (str "![" alt-text "](" url ")")))


(defn link-ref
  "Get the reference for a link."
  ([url]
   (image-ref url "link text"))
  ([url link-text]
   (str "[" link-text "](" url ")")))


(defn ref-pattern
  "Get a pattern for the `url`."
  [url]
  (re-pattern (str " ?!?\\[.*?\\]\\(" url "\\)")))
