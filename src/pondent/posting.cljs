(ns pondent.posting
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [goog.crypt :as crypt]
            [goog.crypt.base64 :as base64]
            [goog.string :as gstring]
            [pondent.file :as file]
            [pondent.github :as github]
            [pondent.time :as time]
            [promesa.core :as p]))


;; Specs

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


(s/fdef file-path :args (s/cat :dir ::path :filename ::filename) :ret ::path)
(s/fdef format-date :args (s/cat :date ::date) :ret string?)
(s/fdef format-title :args (s/cat :title (s/nilable string?)) :ret (s/nilable string?))
(s/fdef format-categories :args (s/cat :categories (s/nilable string?)) :ret (s/nilable string?))
(s/fdef format-body :args (s/cat :body ::content) :ret string?)
(s/fdef format-post :args (s/cat :body ::content :date ::date :title (s/nilable string?) :categories (s/nilable string?)) :ret string?)
(s/fdef input-errors :args (s/keys :req-un [::content ::slug]) :ret map?)
(s/fdef loose-images :args (s/cat :files (s/nilable seq?) :content (s/nilable ::content) :settings :settings) :ret string?)
(s/fdef post-path :args (s/cat :dir ::path :date ::date :slug ::slug) :ret ::path)
(s/fdef success? :args (s/cat :result (s/nilable map?)) :ret boolean?)
(s/fdef uploaded? :args (s/cat :result (s/nilable map?)) :ret boolean?)
(s/fdef create-post :args (s/cat :post map? :settings ::settings) :ret ::promise)
(s/fdef create-file :args (s/cat :file map? :settings ::settings) :ret ::promise)


;; General functions

(defn file-path
  "Construct a path to the file."
  [dir filename]
  (str dir filename))


(defn format-date
  "Format `date` into a form appropriate for frontmatter."
  [date]
  (str "date: " (time/date->str date "yyyy-MM-dd HH:mm:ss Z\n")))


(defn format-title
  "Format `title` into a form appropriate for frontmatter."
  [title]
  (when (not (string/blank? title)) (str "title: \"" title "\"\n")))


(defn format-categories
  "Format `categories` into a form appropriate for frontmatter."
  [categories]
  (when (not (string/blank? categories))
    (str "categories: ["
         (->> (string/split categories #" ")
              (map #(str "\"" % "\""))
              (string/join ", "))
         "]\n")))


(defn format-body
  "Format `body` into a form appropriate for frontmatter."
  [body]
  (str (string/trim-newline body) "\n"))


(defn format-post
  "Format a post for Jekyll."
  [body date title categories]
  (str "---\n"
       (format-date date)
       (format-title title)
       (format-categories categories)
       "---\n\n"
       (format-body body)))


(defn input-errors
  "Return any errors in the input."
  [{:keys [content slug]}]
  (cond
    (string/blank? content) {:kind :missing-content}
    (string/blank? slug)    {:kind :missing-slug}))


(defn loose-images
  "Return a string of HTML with loose images or `nil` if there are no loose
  images."
  [files content settings]
  (let [loose?       #(and (file/image? %)
                           (or (string/blank? content)
                               (nil? (string/index-of content (file/url % settings)))))
        loose-images (filter loose? files)
        image-refs   (map #(str "<img src=\"" (file/url % settings) "\">\n") loose-images)]
    (when (not (empty? image-refs))
      (str "\n\n<div class=\"images\">\n" (string/join image-refs) "</div>"))))


(defn post-path
  "Construct a path to the post."
  [dir date slug]
  (str dir (time/date->str date "yyyy-MM-dd") "-" slug ".md"))


(defn success?
  "Returns `true` if `result` represents success."
  [result]
  (and (map? result) (= :success (:kind result))))


(defn uploaded?
  "Returns `true` if `result` represents a successful upload."
  [result]
  (if (nil? result)
    nil
    (success? result)))


;; Posting functions

(defn create-post
  "Create the `post` using the `settings`."
  [post settings]
  (if-let [error (input-errors post)]
    (p/resolved error)
    (let [date (or (time/str->date (:date post))
                   (time/now))
          content (-> (:content post)
                      (format-post date (:title post) (:categories post))
                      crypt/stringToUtf8ByteArray
                      base64/encodeByteArray)
          path (post-path (:posts-dir settings) date (:slug post))
          commit-message (:posts-commit settings)]
      (-> (github/create-file {:content content :path path :commit-message commit-message}
                              settings)
          (p/then (fn [x]
                    (if (= :success (:status x))
                      {:kind :success}
                      {:kind :save-failure :error x})))))))


(defn create-file
  "Create the `file` using the `settings`."
  [file settings]
  (let [content (:content file)
        path    (file-path (str (:uploads-dir settings) (:year file) "/") (:filename file))
        commit-message (:uploads-commit settings)]
    (-> (github/create-file {:content content :path path :commit-message commit-message}
                            settings)
        (p/then (fn [x]
                  (if (= :success (:status x))
                    {:kind :success}
                    {:kind :save-failure :error x}))))))
