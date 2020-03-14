(ns pondent.file
  (:require [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.path :as path]
            [pondent.time :as time]
            [promesa.core :as p]))


(defn year
  "Get the year `file`was uploaded."
  [file]
  (-> file :date (time/date->str "yyyy")))


(defn filename
  "Get the name of `file`."
  [file]
  (-> file :object .-name))


(defn hashed-name
  "Get the hashed name of the file."
  [file]
  (let [base (-> file :id str gstring/hashCode)
        extension (-> file filename path/extension)]
    (str base "." extension)))


(defn image?
   "Check if `file` is an image."
   [file]
   (case (-> file filename path/extension string/lower-case)
     ("jpg" "jpeg" "png" "gif" "webp" "heif") true
     false))

(defn read-async
  "Return a Promesa promise that reads `file`."
  [file]
  (p/create
    (fn [res rej]
      (let [reader (js/FileReader.)]
        (set! (.-onload reader)
              (fn [x]
                (let [result   (-> x .-target .-result)
                      start    (+ 1 (string/index-of result ","))
                      content  (subs result start)]
                  (res {:filename (hashed-name file)
                        :year (year file)
                        :content content}))))
        (set! (.-onerror reader) rej)
        (.readAsDataURL reader (:object file))))))


(defn url
  "Get the file URL."
  [file settings]
  (str (:uploads-url settings) (year file) "/" (hashed-name file)))
