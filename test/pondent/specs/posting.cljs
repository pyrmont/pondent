(ns pondent.specs.posting
  (:require [clojure.spec.alpha :as s]))


(def filename-regex #"[^/]")
(def url-regex #"https?://[\w.-]+[\w./%#?&=-]+")


(s/def ::content string?)
(s/def ::date #(instance? goog.date.DateTime %))
(s/def ::filename (s/and string? #(re-matches filename-regex %)))
(s/def ::path string?)
(s/def ::promise #(instance? js/Promise %))
(s/def ::settings (s/keys :req-un [::owner ::repo ::gh-user ::gh-password ::gh-token]))
(s/def ::slug string?)
(s/def ::url (s/and string? #(re-matches url-regex %)))


(s/fdef pondent.posting/create-file :args (s/cat :file map? :settings ::settings) :ret ::promise)
(s/fdef pondent.posting/create-post :args (s/cat :post map? :settings ::settings) :ret ::promise)
(s/fdef pondent.posting/file-path :args (s/cat :dir ::path :filename ::filename) :ret ::path)
(s/fdef pondent.posting/format-categories :args (s/cat :categories (s/nilable string?)) :ret (s/nilable string?))
(s/fdef pondent.posting/format-body :args (s/cat :body ::content) :ret string?)
(s/fdef pondent.posting/format-date :args (s/cat :date ::date) :ret string?)
(s/fdef pondent.posting/format-post :args (s/cat :body ::content :date ::date :title (s/nilable string?) :categories (s/nilable string?)) :ret string?)
(s/fdef pondent.posting/format-title :args (s/cat :title (s/nilable string?)) :ret (s/nilable string?))
(s/fdef pondent.posting/input-errors :args (s/keys :req-un [::content ::slug]) :ret map?)
(s/fdef pondent.posting/loose-images :args (s/cat :files (s/nilable seq?) :content (s/nilable ::content) :settings :settings) :ret string?)
(s/fdef pondent.posting/post-path :args (s/cat :dir ::path :date ::date :slug ::slug) :ret ::path)
(s/fdef pondent.posting/success? :args (s/cat :result (s/nilable map?)) :ret boolean?)
(s/fdef pondent.posting/uploaded? :args (s/cat :result (s/nilable map?)) :ret boolean?)
