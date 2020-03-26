(ns pondent.specs.markdown
  (:require [clojure.spec.alpha :as s]))


(def url-regex #"https?://[\w.-]+[\w./%#?&=-]+")


(s/def ::length #(not (neg-int? %)))
(s/def ::meta-length #{0 23})
(s/def ::url (s/and string? #(re-matches url-regex %)))


(s/fdef pondent.markdown/chars-left :args (s/cat :text string? :max-chars int?) :ret int?)
(s/fdef pondent.markdown/image-ref :args (s/cat :url ::url :alt-text (s/? string?)) :ret string?)
(s/fdef pondent.markdown/link-ref :args (s/cat :url ::url :link-text (s/? string?)) :ret string?)
(s/fdef pondent.markdown/meta-length :args (s/cat :open (s/nilable string?) :close string?) :ret (s/nilable ::meta-length))
(s/fdef pondent.markdown/meta-match? :args (s/cat :open (s/nilable string?) :close string?) :ret boolean?)
(s/fdef pondent.markdown/next-plain-char :args (s/cat :text string? :start ::length) :ret ::length)
(s/fdef pondent.markdown/num-plain-chars :args (s/cat :text string?) :ret ::length)
(s/fdef pondent.markdown/ref-pattern :args (s/cat :url ::url) :ret regexp?)
