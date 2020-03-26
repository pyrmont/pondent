(ns pondent.specs.file
  (:require [clojure.spec.alpha :as s]))


(def url-regex #"https?://[\w./]+")


(s/def ::file #(instance? js/File %))
(s/def ::filename string?)
(s/def ::year (s/and string? #(re-matches #"\d{4}" %)))
(s/def ::ext string?)
(s/def ::url (s/and string? #(re-matches url-regex %)))
(s/def ::uploads-url ::url)
(s/def ::promise #(instance? js/Promise %))


(s/fdef pondent.file/filename :args (s/cat :file ::file) :ret ::filename)
(s/fdef pondent.file/year :args (s/cat :file ::file) :ret (s/nilable ::year))
(s/fdef pondent.file/extension :args (s/cat :file ::file :lower-case? (s/? boolean?)) :ret ::ext)
(s/fdef pondent.file/hashed-name :args (s/cat :file ::file) :ret ::filename)
(s/fdef pondent.file/url :args (s/cat :file ::file :settings (s/keys :req-un [::uploads-url])) :ret ::url)
(s/fdef pondent.file/image? :args (s/cat :file ::file) :ret boolean?)
(s/fdef pondent.file/movie? :args (s/cat :file ::file) :ret boolean?)
(s/fdef pondent.file/read-async :args (s/cat :file ::file) :ret ::promise)
