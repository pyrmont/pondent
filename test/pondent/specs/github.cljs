(ns pondent.specs.github
  (:require [clojure.spec.alpha :as s]))


(def url-path-regex #"[\w./%#?&=-]+")
(def url-regex #"https?://[\w.-]+[\w./%#?&=-]+")
(def slug-regex #"[\w-]")


(s/def ::token (s/nilable string?))
(s/def ::user (s/nilable string?))
(s/def ::password (s/nilable string?))
(s/def ::json string?)
(s/def ::url-path (s/and string? #(re-matches url-path-regex %)))
(s/def ::url (s/and string? #(re-matches url-regex %)))
(s/def ::promise #(instance? js/Promise %))
(s/def ::content string?)
(s/def ::path ::url-path)
(s/def ::commit-message string?)
(s/def ::owner (s/and string? #(re-matches slug-regex %)))
(s/def ::repo (s/and string? #(re-matches slug-regex %)))
(s/def ::gh-user (s/and string? #(re-matches slug-regex %)))
(s/def ::gh-password string?)
(s/def ::file-data (s/keys :req-un [::content ::path ::commit-message]))
(s/def ::settings (s/keys :req-un [::owner ::repo ::gh-user ::gh-password ::gh-token]))


(s/fdef pondent.github/add-headers :args (s/cat :request map? :opts (s/keys :req-un [::token ::user ::password])) :ret map?)
(s/fdef pondent.github/body->map :args (s/cat :body ::json) :ret map?)
(s/fdef pondent.github/auth-token-via-proxy :args (s/cat :proxy-url ::url :code string?) :ret ::promise)
(s/fdef pondent.github/auth-url :args (s/cat :client-id string?) :ret ::url)
(s/fdef pondent.github/authd? :args (s/cat :gh-token string?) :ret ::promise)
(s/fdef pondent.github/app-url :args (s/cat :client-id string?) :ret ::url)
(s/fdef pondent.github/create-file :args (s/cat :data ::file-data :settings ::settings) :ret ::promise)
(s/fdef pondent.github/GET :args (s/cat :url ::url :body ::json :opts map?) :ret ::promise)
(s/fdef pondent.github/github-url :args (s/cat :url-part ::url-path) :ret ::url)
(s/fdef pondent.github/map->body :args (s/cat :data map?) :ret ::json)
(s/fdef pondent.github/process-create-response :args (s/cat :response map?) :ret map?)
(s/fdef pondent.github/PUT :args (s/cat :url ::url :body ::json :opts map?) :ret ::promise)
