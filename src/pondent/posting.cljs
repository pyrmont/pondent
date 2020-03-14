(ns pondent.posting
  (:require [clojure.string :as string]
            [goog.crypt.base64 :as base64]
            [goog.string :as gstring]
            [pondent.file :as file]
            [pondent.github :as github]
            [pondent.time :as time]
            [promesa.core :as p]))


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
                      base64/encodeString)
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
