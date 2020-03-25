(ns pondent.file
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [goog.string :as gstring]
            [goog.string.path :as path]
            [pondent.time :as time]
            [promesa.core :as p]))


;; Specs

(def url-regex #"https?://[\w./]+")


(s/def ::file #(instance? js/File %))
(s/def ::filename string?)
(s/def ::year (s/and string? #(re-matches #"\d{4}" %)))
(s/def ::ext string?)
(s/def ::url (s/and string? #(re-matches url-regex %)))
(s/def ::uploads-url ::url)
(s/def ::promise #(instance? js/Promise %))


(s/fdef filename :args (s/cat :file ::file) :ret ::filename)
(s/fdef year :args (s/cat :file ::file) :ret (s/nilable ::year))
(s/fdef extension :args (s/cat :file ::file :lower-case? (s/? boolean?)) :ret ::ext)
(s/fdef hashed-name :args (s/cat :file ::file) :ret ::filename)
(s/fdef url :args (s/cat :file ::file :settings (s/keys :req-un [::uploads-url])) :ret ::url)
(s/fdef image? :args (s/cat :file ::file) :ret boolean?)
(s/fdef movie? :args (s/cat :file ::file) :ret boolean?)
(s/fdef read-async :args (s/cat :file ::file) :ret ::promise)


;; Independent properties

(defn filename
  "Get the name of `file`."
  [file]
  (-> file :object .-name))


(defn year
  "Get the year `file` was uploaded."
  [file]
  (-> file :date (time/date->str "yyyy")))


;; Dependent properties

(defn extension
  "Get the extension of `file`. By default, the extension is transformed to
  lower-case."
  ([file]
   (extension file true))
  ([file lower-case?]
   (let [ext (-> file filename path/extension)]
     (if lower-case? (string/lower-case ext) ext))))


;; Transformations

(defn hashed-name
  "Get the hashed name of the file."
  [file]
  (let [base (-> file :id str gstring/hashCode)
        extension (-> file (extension false))]
    (str base "." extension)))


(defn url
  "Get the file URL."
  [file settings]
  (str (:uploads-url settings) (year file) "/" (hashed-name file)))


;; Miscellaneous

(defn image?
   "Check if `file` is an image."
   [file]
   (contains? #{"gif" "heif" "jpg" "jpeg" "png" "webp"}
             (-> file extension)))


(defn movie?
  " Check if `file` is a movie."
  [file]
  (contains? #{"avi" "mkv" "mov" "mp4"}
             (-> file extension)))


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
